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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.upgrade.steps.propertyvalidation;

import java.security.PrivilegedAction;

import javax.inject.Inject;

import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeStepInfo;

import com.iplanet.sso.SSOToken;

/**
 * An Upgrade step for updating the validation in the RestSecurity Service.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class RestSecurityValidationUpgradeStep extends AbstractNewRequiredAttributeUpgradeStep {

    private static final String SERVICE_NAME = "RestSecurity";
    private static final String NEW_VALIDATOR_NAME = "RequiredValueValidator";

    private enum Default implements DefaultPropertyValue {
        PROTECTED_USER_ATTRIBUTE_ALIAS("forgerockRESTSecurityProtectedUserAttributes", null);

        private String fieldName;
        private String newDefault;

        Default(String field, String newDefault) {
            this.fieldName = field;
            this.newDefault = newDefault;
        }

        @Override
        public String getFieldName() {
            return fieldName;
        }

        @Override
        public String getNewDefault() {
            return newDefault;
        }
    }


    @Inject
    public RestSecurityValidationUpgradeStep(
            PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
    }

    @Override
    protected DefaultPropertyValue[] getNewRequiredAttributeDefaultValues() {
        return Default.values();
    }

    @Override
    protected String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected String getValidatorName() {
        return NEW_VALIDATOR_NAME;
    }
}
