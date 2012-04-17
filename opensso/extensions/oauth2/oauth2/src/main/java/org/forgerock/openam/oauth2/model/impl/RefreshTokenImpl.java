/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2012 ForgeRock. All rights reserved.
 */

package org.forgerock.openam.oauth2.model.impl;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.restlet.ext.oauth2.OAuth2;
import org.forgerock.restlet.ext.oauth2.model.RefreshToken;
import org.forgerock.restlet.ext.oauth2.model.SessionClient;
import org.forgerock.restlet.ext.oauth2.model.Token;

import java.util.Set;

/**
 * TODO Description.
 */
public class RefreshTokenImpl extends TokenImpl implements RefreshToken {

    private String parent;

    /**
     * TODO Description.
     *
     * @param id         TODO Description
     * @param parent     TODO Description
     * @param userID     TODO Description
     * @param client     TODO Description
     * @param realm      TODO Description
     * @param scope      TODO Description
     * @param expireTime TODO Description
     */
    public RefreshTokenImpl(String id, String parent, String userID, SessionClient client,
                            String realm, Set<String> scope, long expireTime) {
        super(id, userID, client, realm, scope, expireTime);
        setType();
        setParentToken(parent);
    }

    /**
     * TODO Description.
     *
     * @param id         TODO Description
     * @param scope      TODO Description
     * @param expireTime TODO Description
     * @param token      TODO Description
     */
    public RefreshTokenImpl(String id, Set<String> scope, long expireTime, Token token) {
        super(id, token.getUserID(), token.getClient(), token.getRealm(), scope, expireTime);
        setType();
    }

    /**
     * TODO Description.
     *
     * @param id    TODO Description
     * @param value TODO Description
     */
    public RefreshTokenImpl(String id, JsonValue value) {
        super(id, value);
        setType();
    }

    /**
     * TODO Description.
     *
     * @param parent TODO Description
     */
    public void setParentToken(String parent) {
        this.put(OAuth2.StoredToken.PARENT, parent);
    }

    @Override
    public String getParentToken() {
        return this.get(OAuth2.StoredToken.PARENT).asString();
    }

    /**
     * TODO Description.
     */
    protected void setType() {
        this.put(OAuth2.StoredToken.TYPE, OAuth2.Params.REFRESH_TOKEN);
    }

}
