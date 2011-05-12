The wamulator uses a self generated certificate authority certificate created with the
org.lds.sso.appwrap.certs.CaCertificateGenerator run as a java application.
This generates two files in the root of the project ca-x509-cert.pem and
ca-pkcs8-key.pem both in Privacy Enhanced Mail (PEM) format. These are then
respectively renamed ca-cert.pem and ca-private-key.pem and placed in the
/src/main/resources folder so that they will be available on the classpath
at runtime. The generated certificate has a valid period of twenty years from 
its creation. 

This approach was taken so that the CA certificate would stay valid and not
change so that if users wished to import it into their browsers or trust stores
they could do so and thereby accept the auto generated site certificates that use
this CA when the wamulator is hit without having the annoying pop-up dialogs
every time a new site certificate is generated.