/*
 *   R Service Bus
 *   
 *   Copyright (c) Copyright of OpenAnalytics BVBA, 2010-2011
 *
 *   ===========================================================================
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.openanalytics.rsb.component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.cxf.common.util.Base64Utility;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import eu.openanalytics.rsb.Constants;
import eu.openanalytics.rsb.Util;

/**
 * Serves R job process result files.<br/>
 * <i>NB. Could very well be replaced with a static file serving context on a frontal web
 * server.</i>
 * 
 * @author "OpenAnalytics <rsb.development@openanalytics.eu>"
 */
@Component("resultResource")
@Produces("application/octet-stream")
@Path("/" + Constants.RESULT_PATH + "/{applicationName}/{resultFileName}")
public class ResultResource extends AbstractComponent {
    /**
     * Serves a single result file.
     * 
     * @param applicationName
     * @param resultFileName
     * @return
     * @throws IOException
     */
    @GET
    public Response getResult(@PathParam("applicationName") final String applicationName,
            @PathParam("resultFileName") final String resultFileName) throws IOException {

        final File resultFile = getResultFileOrDie(applicationName, resultFileName);

        final ResponseBuilder rb = Response.ok();
        addEtagHeader(applicationName, resultFileName, rb);
        rb.entity(new StreamingOutput() {
            public void write(final OutputStream output) throws IOException, WebApplicationException {
                FileCopyUtils.copy(new FileInputStream(resultFile), output);
            }
        });
        return rb.build();
    }

    /**
     * Provides HTTP meta-information only for a single result file.
     * 
     * @param applicationName
     * @param resultFileName
     * @return
     */
    @HEAD
    public Response getResultMeta(@PathParam("applicationName") final String applicationName,
            @PathParam("resultFileName") final String resultFileName) {

        final File resultFile = getResultFileOrDie(applicationName, resultFileName);
        final ResponseBuilder rb = Response.noContent();
        addContentLengthHeader(resultFile, rb);
        addEtagHeader(applicationName, resultFileName, rb);
        return rb.build();
    }

    private void addEtagHeader(final String applicationName, final String resultFileName, final ResponseBuilder rb) {
        rb.header(HttpHeaders.ETAG, getEtag(applicationName, resultFileName));
    }

    private void addContentLengthHeader(final File resultFile, final ResponseBuilder rb) {
        rb.header(HttpHeaders.CONTENT_LENGTH, Long.toString(resultFile.length()));
    }

    private File getResultFileOrDie(final String applicationName, final String resultFileName) {
        if (!Util.isValidApplicationName(applicationName)) {
            throw new IllegalArgumentException("Invalid application name: " + applicationName);
        }

        final File resultFile = new File(getApplicationResultDirectory(applicationName), resultFileName);

        if (!resultFile.exists()) {
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).build());
        }

        return resultFile;
    }

    // exposed for unit testing
    static String getEtag(final String applicationName, final String resultFileName) {
        return Base64Utility.encode((applicationName + "/" + resultFileName).getBytes());
    }
}
