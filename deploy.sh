echo 'maven build'
./mvnw clean install -DskipTests

echo 'scp jar file to prod-1'
scp -i ~/.ssh/aws-mbslaw-key.pem target/*.jar ubuntu@43.200.165.243:/apps/tbm-admin/lib

echo 'run restart'
ssh -i ~/.ssh/aws-mbslaw-key.pem ubuntu@43.200.165.243 'cd /apps/tbm-admin/bin; ./restart.sh'