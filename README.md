# Azure Member Provider - Prototype
This application invokes _Microsoft Azure REST API_ to retrieve the hostnames of the web applications that are running inside a resource group.


## Setting up the Azure environment
If you haven't got a Microsoft Azure account yet, you can create one [here](https://azure.microsoft.com/en-us/free/) and download [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest).

Log to Azure :

    $ az login

Create Service Principal with the desired password :

    $ az ad sp create-for-rbac --name "azure-auth" --password "password"

Create Resource Group :

    $ az group create --name=tomcat-in-the-cloud-group --location=westeurope

Create Azure Container Registry :

    $ az acr create --admin-enabled --resource-group tomcat-in-the-cloud-group --location westeurope --name tomcatinthecloud --sku Basic


## Deploy a demo webapp to Azure
### Configuring maven
Get password for Container Registry :

    $ az acr credential show --name tomcatinthecloud --query passwords[0]

Add the following to your _$HOME/.m2/settings.xml_ (or _/etc/maven/settings.xml_) file :

    <servers>
       ..
       <!-- Connection to Service Principal -->
       <server>
          <id>tomcatinthecloud</id>
          <username>tomcatinthecloud</username>
          <password>CONTAINER_REGISTRY_PASSWORD</password>
          <configuration>
            <email>AZURE_ACCOUNT_EMAIL_ADDRESS</email>
          </configuration>
       </server>

       <!-- Connection to Service Principal -->
       <server>
         <id>azure-auth</id>
          <configuration>
             <client>CLIENT_ID</client>
             <tenant>TENANT_ID</tenant>
             <key>password</key>
             <environment>AZURE</environment>
          </configuration>
       </server>
       ..
    </servers>

You can refer to [_Useful commands for Azure CLI_](https://github.com/maxime-beck/prototype_azureMemberProvider#useful-commands-for-azure-cli) to get this values

### Build and deploy the webapp
#### Requirements
[Install and configure docker](https://docs.docker.com/engine/installation/linux/linux-postinstall) on your local machine.

#### Procedure
Clone the demo app repo :

    $ git clone -b private-registry https://github.com/microsoft/gs-spring-boot-docker

Change directory :

    $ cd gs-spring-boot-docker/complete

Edit _pom.xml_ as follow :

    <properties>
      <azure.containerRegistry>tomcatinthecloud</azure.containerRegistry>
      ...
    </properties>
    
    ...
    
    <build>
      <plugins>
        <plugin>
          <groupId>com.microsoft.azure</groupId>
          <artifactId>azure-webapp-maven-plugin</artifactId>
          <version>0.1.3</version>
          <configuration>
            ...
            <resourceGroup>tomcat-in-the-cloud-group</resourceGroup>
            ...
          <configuration>
        <plugin>
      <plugins>
    </build>

Build and deploy the webapp on Azure :

    $ mvn clean package docker:build -DpushImage azure-webapp:deploy


## Build and run
### Configure
In _src/main/java/com/azure/test/PublicClient.java_, replace the following constants by the corresponding information :

    private final static String SUBSCRIPTION_ID = "<subscription id>";
    private final static String TENANT_ID = "<tenant id>";
    private final static String RESOURCEGROUPNAME = "<resource group name>";
    private final static String CLIENT_ID = "<client id>";
    private final static String PASSWORD = "<service principal password>";

Build the application :

    $ mvn install

And run it :

    $ java -jar target/azureRestAPI-1.0-SNAPSHOT-jar-with-dependencies.jar


# Useful commands for Azure CLI
Get password for Container Registry :

    $ az acr credential show --name tomcatinthecloud --query passwords[0]

Get the subscription ID (id field) and the tenant ID :

    $ az account show

Get Client ID (appId field) :

    $ az ad sp list --query "[?displayName == 'azure-auth']"

# References
_Setting up Azure_ :

https://docs.microsoft.com/en-us/azure/app-service/containers/app-service-web-deploy-spring-boot-app-from-container-registry-using-maven-plugin

_Deploy test webapp_ :

https://azure.microsoft.com/en-us/blog/maven-deploy-java-web-apps-to-azure/
