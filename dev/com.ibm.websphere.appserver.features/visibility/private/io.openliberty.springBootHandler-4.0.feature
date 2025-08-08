-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.springBootHandler-4.0
visibility=private
-features=io.openliberty.springBootHandler4.0.ee-10.0; ibm.tolerates:="11.0",\
	io.openliberty.noShip-1.0
-bundles=\
 com.ibm.ws.app.manager.springboot.jakarta, \
 com.ibm.ws.springboot.support.shutdown, \
 com.ibm.ws.springboot.utility
-files=\
 bin/tools/ws-springbootutil.jar, \
 bin/springBootUtility; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/springBootUtility.bat
kind=noship
edition=full
singleton=true
IBM-API-Package: \
 com.ibm.ws.app.manager.springboot.container.config; type="internal", \
 com.ibm.ws.app.manager.springboot.container; type="internal"
