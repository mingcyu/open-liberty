# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="ibmcom/cloudant-developer:2.0.1"

FROM ${BASE_IMAGE}

#Contains vulnerable log4j library for features not needed for testing
RUN rm -rf /opt/cloudant/search/
RUN rm -rf /opt/cloudant/etc/log4j.properties
