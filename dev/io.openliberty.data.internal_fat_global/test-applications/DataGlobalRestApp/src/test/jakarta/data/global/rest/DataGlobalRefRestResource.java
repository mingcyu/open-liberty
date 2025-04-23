/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.global.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
@Path("/referral")
public class DataGlobalRefRestResource {
    @Inject
    Referrals referrals;

    @GET
    @Path("/email/{email}")
    @Produces(MediaType.APPLICATION_JSON)
    public Referral getReferral(@PathParam("email") String email) {

        return referrals.findById(email)
                        .orElseThrow(() -> new NotFoundException("email: " + email));
    }

    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/save")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Referral saveReferral(Referral referral) {

        return referrals.save(referral);
    }
}
