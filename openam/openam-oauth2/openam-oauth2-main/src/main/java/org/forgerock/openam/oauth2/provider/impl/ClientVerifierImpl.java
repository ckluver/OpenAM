/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [2012] [Forgerock Inc]"
 */

package org.forgerock.openam.oauth2.provider.impl;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.*;
import com.sun.identity.security.AdminTokenAction;
import org.forgerock.openam.oauth2.OAuth2;
import org.forgerock.openam.oauth2.model.impl.ClientApplicationImpl;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.ClientApplication;
import org.forgerock.openam.oauth2.provider.ClientVerifier;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.Request;
import org.restlet.Response;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import com.sun.identity.shared.encode.Hash;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

public class ClientVerifierImpl implements ClientVerifier{

    private String realm = null;

    @Override
    public ClientApplication verify(Request request, Response response){
        if (OAuth2Utils.debug.messageEnabled()){
            OAuth2Utils.debug.message("ClientVerifierImpl::Verifying client application");
        }
        ClientApplication client = null;
        realm = OAuth2Utils.getRealm(request);
        if (request.getChallengeResponse() != null) {
            client = verify(request.getChallengeResponse());
        } else {
            String client_secret =
                    OAuth2Utils.getRequestParameter(request, OAuth2.Params.CLIENT_SECRET,
                            String.class);
            String client_id =
                    OAuth2Utils.getRequestParameter(request, OAuth2.Params.CLIENT_ID,
                            String.class);
            if (client_secret != null){
                client = verify(client_id, client_secret);
            } else {
                client = findClient(client_id);
            }
        }
        return client;
    }

    private ClientApplication verify(ChallengeResponse challengeResponse)
            throws OAuthProblemException{
        String client_id = challengeResponse.getIdentifier();
        String client_secret = String.valueOf(challengeResponse.getSecret());
        return verify(client_id, client_secret);
    }

    private ClientApplication verify(String client_id, String client_secret)
            throws OAuthProblemException{
        ClientApplication user = null;
        try {
            AMIdentity ret = authenticate(client_id, client_secret.toCharArray());
            if (ret == null){
                OAuth2Utils.debug.error("ClientVerifierImpl::Unable to verify client password: " +
                    client_secret);
                throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(null, "Unauthorized client");
            }
            user = new ClientApplicationImpl(ret);
        } catch (Exception e){
            OAuth2Utils.debug.error("ClientVerifierImpl::Unable to verify client", e);
            throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(null, "Unauthorized client");
        }
        return user;
    }

    private AMIdentity authenticate(String username, char[] password) {

        AMIdentity ret = null;
        try {
            AuthContext lc = new AuthContext(realm);
            lc.login();
            while (lc.hasMoreRequirements()) {
                Callback[] callbacks = lc.getRequirements();
                ArrayList missing = new ArrayList();
                // loop through the requires setting the needs..
                for (int i = 0; i < callbacks.length; i++) {
                    if (callbacks[i] instanceof NameCallback) {
                        NameCallback nc = (NameCallback) callbacks[i];
                        nc.setName(username);
                    } else if (callbacks[i] instanceof PasswordCallback) {
                        PasswordCallback pc = (PasswordCallback) callbacks[i];
                        pc.setPassword(password);
                    } else {
                        missing.add(callbacks[i]);
                    }
                }
                // there's missing requirements not filled by this
                if (missing.size() > 0) {
                    throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                            "Missing requirements");
                }
                lc.submitRequirements(callbacks);
            }

            if (OAuth2Utils.debug.messageEnabled()) {
                OAuth2Utils.debug.message("ClientVerifierImpl::authenticate returning an InvalidCredentials"
                        + " exception for invalid passwords.");
            }

            // validate the password..
            if (lc.getStatus() == AuthContext.Status.SUCCESS) {
                try {
                    ret = IdUtils.getIdentity(lc.getSSOToken());
                } catch (Exception e) {
                    OAuth2Utils.debug.error( "ClientVerifierImpl::authContext: "
                            + "Unable to get SSOToken", e);
                    // we're going to throw a generic error
                    // because the system is likely down..
                    throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
                }
            }
        } catch (AuthLoginException le) {
            OAuth2Utils.debug.error("ClientVerifierImpl::authContext AuthException", le);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, le);
        }
        return ret;
    }

    @Override
    public Collection<ChallengeScheme> getRequiredAuthenticationScheme(String client_id){

        return null;
    }

    private ClientApplication findClient(String client_id){

        ClientApplication user = null;
        try {
            AMIdentity id = getIdentity(client_id);
            user = new ClientApplicationImpl(id);
        } catch (Exception e){
            return user;
        }
        return user;
    }

    private AMIdentity getIdentity(String uName) throws OAuthProblemException {
        SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
        AMIdentity theID = null;

        try {
            AMIdentityRepository amIdRepo = new AMIdentityRepository(token, realm);

            IdSearchControl idsc = new IdSearchControl();
            idsc.setRecursive(true);
            idsc.setAllReturnAttributes(true);
            // search for the identity
            Set<AMIdentity> results = Collections.EMPTY_SET;
            idsc.setMaxResults(0);
            IdSearchResults searchResults =
                    amIdRepo.searchIdentities(IdType.AGENT, uName, idsc);
            if (searchResults != null) {
                results = searchResults.getSearchResults();
            }

            if (results == null || results.size() != 1) {
                throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(null,
                                                                                 "Not able to get client from OpenAM");

            }

            theID = results.iterator().next();

            //if the client is deactivated return null
            if (theID.isActive()){
                return theID;
            } else {
                return null;
            }
        } catch (Exception e){
            OAuth2Utils.debug.error("ClientVerifierImpl::Unable to get client AMIdentity: ", e);
            throw OAuthProblemException.OAuthError.UNAUTHORIZED_CLIENT.handle(null, "Not able to get client from OpenAM");
        }
    }
}
