-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.connector-1.6
WLP-DisableAllFeatures-OnConflict: false
visibility=protected
singleton=true
IBM-API-Package: javax.resource; type="spec", \
 javax.resource.cci; type="spec", \
 javax.resource.spi; type="spec", \
 javax.resource.spi.endpoint; type="spec", \
 javax.resource.spi.security; type="spec", \
 javax.resource.spi.work; type="spec"
-features=com.ibm.websphere.appserver.eeCompatible-6.0, \
  com.ibm.websphere.appserver.javax.connector.internal-1.6
-bundles=com.ibm.websphere.javaee.connector.1.6; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.resource:javax.resource-api:1.7"
kind=ga
edition=base
WLP-Activation-Type: parallel
