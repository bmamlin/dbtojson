#!/usr/bin/env groovy

/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at 
 * http://mozilla.org/MPL/2.0/.
 */

/**
 * DbToJson.groovy
 *
 * This script uses DbMeta to generate a JSON file representing the database schema of
 * a version of OpenMRS Platform or Reference Application.
 *
 * Required environment variables
 * OPENMRS_DISTRO="openmrs-reference-application-distro" or "openmrs-distro-platform"
 * OPENMRS_DISTRO_VERSION=version number
 * DB_URL (e.g., "jdbc:mysql://localhost/dbname")
 * DB_USERNAME
 * DB_PASSWORD
 *
 */

@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')

import groovyx.net.http.*
import org.apache.http.client.*
import org.apache.http.protocol.*
import groovy.transform.Field
import groovy.json.*

def OPENMRS_DISTRO = System.env.OPENMRS_DISTRO
def OPENMRS_DISTRO_VERSION = System.env.OPENMRS_DISTRO_VERSION
def OPENMRS_DISTRO_TYPE = OPENMRS_DISTRO == 'openmrs-reference-application-distro' ? 'refapp' : 'platform'

def DB_URL = System.env.DB_URL
def DB_USERNAME = System.env.DB_USERNAME
def DB_PASSWORD = System.env.DB_PASSWORD
def OUTPUT_FILE = "${OPENMRS_DISTRO_TYPE}-${OPENMRS_DISTRO_VERSION}.json"

// Make sure that HttpClient doesn't perform a redirect
def dontHandleRedirectStrategy = [
  getRedirect : { request, response, context -> null},
  isRedirected : { request, response, context -> false}
]
// Avoid retrying requests (we expect no response when OpenMRS first starting up)
def requestRetryHandler = new HttpRequestRetryHandler() {
    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
        return false
    }
}

@Field def httpBuilder = new HTTPBuilder("http://openmrs:8080")
httpBuilder.client.setRedirectStrategy(dontHandleRedirectStrategy as RedirectStrategy)
httpBuilder.client.setHttpRequestRetryHandler(requestRetryHandler)

enum Status {
  UNAVAILABLE, INITIALIZING, STARTED, UNKNOWN
}

def openmrsStatus() {
  // Execute a GET request and expect a redirect
  try {
    httpBuilder.request(Method.GET, ContentType.TEXT) { req ->
      uri.path = '/openmrs/'
      response.success = { response, reader ->
        if (response.statusLine.statusCode == 302 && response.headers['Location'].value.contains("initialsetup")) {
          // If redirected to initial setup, OpenMRS is initializing
          return Status.INITIALIZING
        } else {
          // Any other redirection happens after OpenMRS has finished initializing
          return Status.STARTED
        }
      }
      response.failure = { response, reader ->
        if (response.statusLine.statusCode == 200) {
          // If we get a 200 without redirection, assume OpenMRS has finished initializing
          return Status.STARTED
        } else {
          return Status.UNKNOWN
        }
      }
    }
  } catch(Exception e) {
    return Status.UNAVAILABLE
  }
}

println("Waiting for OpenMRS to start...")
while ((status = openmrsStatus()) != Status.STARTED) {
  sleep(1000) {
    println("Interrupted. Aborting.")
    System.exit(1)
  }
}

println("OpenMRS has started")

println("Scanning database...")
def tables = DbMeta.parse(DB_URL, DB_USERNAME, DB_PASSWORD)

def schema = [
  distro: OPENMRS_DISTRO_TYPE,
  version: OPENMRS_DISTRO_VERSION,
  tables: tables
]

// Write our output to file system
def out = new File("json/$OUTPUT_FILE")
def generator = new JsonGenerator.Options().excludeNulls().build()
out.write(JsonOutput.prettyPrint(generator.toJson(schema)))
println("Schema written to $OUTPUT_FILE")
