cd ./target
nohup java -jar demo-0.0.1-SNAPSHOT.jar &
cd ../src/main/react
rm -rf node_modules
npm install
npm audit fix
nohup npm start &
exit