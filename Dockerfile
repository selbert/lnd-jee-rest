FROM airhacks/glassfish
COPY ./target/lnd-api.war ${DEPLOYMENT_DIR}
