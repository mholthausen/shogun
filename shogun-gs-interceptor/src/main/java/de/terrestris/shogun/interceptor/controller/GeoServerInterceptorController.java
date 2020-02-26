package de.terrestris.shogun.interceptor.controller;

import de.terrestris.shogun.interceptor.exception.InterceptorException;
import de.terrestris.shogun.interceptor.service.GeoServerInterceptorService;
import de.terrestris.shogun.lib.dto.HttpResponse;
import org.apache.http.HttpException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

@RestController
public class GeoServerInterceptorController {

    public static final String ERROR_MESSAGE = "Error while requesting a GeoServer resource: ";

    protected final Logger LOG = LogManager.getLogger(getClass());

    @Autowired
    protected GeoServerInterceptorService service;

    @RequestMapping(value = {"/geoserver.action", "/geoserver.action/{endpoint}"}, method = {
        RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> interceptGeoServerRequest(HttpServletRequest request, @PathVariable(value = "endpoint", required = false) Optional<String> endpoint) {
        HttpHeaders responseHeaders = new HttpHeaders();
        HttpStatus responseStatus = HttpStatus.OK;
        byte[] responseBody;
        HttpResponse httpResponse;

        try {
            LOG.trace("Trying to intercept a GeoServer resource.");

            httpResponse = this.service.interceptGeoServerRequest(request, endpoint);

            responseStatus = httpResponse.getStatusCode();
            responseBody = httpResponse.getBody();
            responseHeaders = httpResponse.getHeaders();

            LOG.trace("Successfully intercepted a GeoServer resource.");

            return new ResponseEntity<>(responseBody,
                responseHeaders, responseStatus);

        } catch (NullPointerException | IOException | InterceptorException | HttpException | URISyntaxException e) {
            LOG.error(ERROR_MESSAGE + e.getMessage());
            LOG.trace("Full stack trace: ", e);

            responseHeaders.setContentType(MediaType.APPLICATION_JSON);

            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ERROR_MESSAGE + e.getMessage(),
                e
            );
        }

    }
}