package au.com.dius.pact.model

import groovy.json.JsonSlurper
import groovy.transform.Canonical

/**
 * Interaction between a consumer and a provider
 */
@Canonical
class RequestResponseInteraction implements Interaction {

  String description
  List<ProviderState> providerStates = []
  Request request
  Response response

  String toString() {
    "Interaction: $description\n\tin states ${displayState()}\nrequest:\n$request\n\nresponse:\n$response"
  }

  String displayState() {
    if (providerStates.empty || providerStates.size() == 1 && !providerStates[0].name) {
      'None'
    } else {
      providerStates*.name.join(', ')
    }
  }

  @Override
  @Deprecated
  String getProviderState() {
    providerStates.empty ? null : providerStates.first().name
  }

  boolean conflictsWith(Interaction other) {
    description == other.description &&
      providerStates == other.providerStates &&
      (request != other.request || response != other.response)
  }

  @Override
  @SuppressWarnings('SpaceAroundMapEntryColon')
  Map toMap(PactSpecVersion pactSpecVersion = PactSpecVersion.V3) {
    def interactionJson = [
      description     : description,
      request         : requestToMap(request, pactSpecVersion),
      response        : responseToMap(response, pactSpecVersion)
    ]
    if (pactSpecVersion < PactSpecVersion.V3 && providerStates) {
      interactionJson.providerState = providerState
    } else if (providerStates) {
      interactionJson.providerStates = providerStates*.toMap()
    }
    interactionJson
  }

  static Map requestToMap(Request request, PactSpecVersion pactSpecVersion) {
    Map<String, Object> map = [
      method: request.method.toUpperCase() as Object,
      path: request.path as Object
    ]
    if (request.headers) {
      map.headers = request.headers as Map
    }
    if (request.query) {
      map.query = pactSpecVersion >= PactSpecVersion.V3 ? request.query : mapToQueryStr(request.query)
    }
    if (!request.body.missing) {
      map.body = parseBody(request)
    }
    if (request.matchingRules?.notEmpty) {
      map.matchingRules = request.matchingRules.toMap(pactSpecVersion)
    }
    map
  }

  static Map responseToMap(Response response, PactSpecVersion pactSpecVersion) {
    Map<String, Object> map = [status: response.status as Object]
    if (response.headers) {
      map.headers = response.headers as Map
    }
    if (!response.body.missing) {
      map.body = parseBody(response)
    }
    if (response.matchingRules?.notEmpty) {
      map.matchingRules = response.matchingRules.toMap(pactSpecVersion)
    }
    map
  }

  static String mapToQueryStr(Map<String, List<String>> query) {
    query.collectMany { k, v -> v.collect { "$k=${URLEncoder.encode(it, 'UTF-8')}" } }.join('&')
  }

  static parseBody(HttpPart httpPart) {
    if (httpPart.jsonBody() && httpPart.body.present) {
      new JsonSlurper().parseText(httpPart.body.value)
    } else {
      httpPart.body.value
    }
  }

}
