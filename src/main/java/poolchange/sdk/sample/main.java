// Copyright (c) Microsoft and contributors.  All rights reserved.
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package poolchange.sdk.sample;

import com.ea.async.Async;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.netapp.v2020_06_01.*;
import com.microsoft.azure.management.netapp.v2020_06_01.implementation.*;
import com.microsoft.rest.credentials.ServiceClientCredentials;
import poolchange.sdk.sample.common.CommonSdk;
import poolchange.sdk.sample.common.ServiceCredentialsAuth;
import poolchange.sdk.sample.common.Utils;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static com.ea.async.Async.await;

public class main
{
    /**
     * Sample console application that executes CRUD management operations on Azure NetApp Files resources
     * Showcases how to do a Pool Change - Moving an existing volume from one pool to another at a different tier
     * @param args
     */
    public static void main( String[] args )
    {
        Utils.displayConsoleAppHeader();

        try
        {
            Async.init();
            runAsync();
            Utils.writeConsoleMessage("Sample application successfully completed execution");
        }
        catch (Exception e)
        {
            Utils.writeErrorMessage(e.getMessage());
        }

        System.exit(0);
    }

    private static CompletableFuture<Void> runAsync()
    {
        //---------------------------------------------------------------------------------------------------------------------
        // Setting variables necessary for resources creation - change these to appropriate values related to your environment
        //---------------------------------------------------------------------------------------------------------------------
        boolean cleanup = false;

        String subscriptionId = "<subscription id>";
        String location = "eastus";
        String resourceGroupName = "anf01-rg";
        String vnetName = "vnet";
        String subnetName = "anf-sn";
        String anfAccountName = "anfaccount99";
        String capacityPoolNameSource = "pool-source";
        String capacityPoolNameDestination = "pool-destination";
        String capacityPoolServiceLevelSource = "Premium"; // Valid service levels are: Ultra, Premium, Standard
        String capacityPoolServiceLevelDestination = "Standard";
        String volumeName = "volume01";

        long capacityPoolSize = 4398046511104L;  // 4TiB which is minimum size
        long volumeSize = 107374182400L;  // 100GiB - volume minimum size

        // Authenticating using service principal, refer to README.md file for requirement details
        ServiceClientCredentials credentials = ServiceCredentialsAuth.getServicePrincipalCredentials(System.getenv("AZURE_AUTH_LOCATION"));
        if (credentials == null)
        {
            return CompletableFuture.completedFuture(null);
        }

        // Instantiating a new ANF management client
        Utils.writeConsoleMessage("Instantiating a new Azure NetApp Files management client...");
        AzureNetAppFilesManagementClientImpl anfClient = new AzureNetAppFilesManagementClientImpl(credentials);
        anfClient.withSubscriptionId(subscriptionId);
        Utils.writeConsoleMessage("Api Version: " + anfClient.apiVersion());

        //---------------------------
        // Creating ANF resources
        //---------------------------

        //---------------------------
        // Create ANF Account
        //---------------------------
        Utils.writeConsoleMessage("Creating Azure NetApp Files Account...");

        String[] accountParams = {resourceGroupName, anfAccountName};
        NetAppAccountInner anfAccount = await(CommonSdk.getResourceAsync(anfClient, accountParams, NetAppAccountInner.class));
        if (anfAccount == null)
        {
            NetAppAccountInner newAccount = new NetAppAccountInner();
            newAccount.withLocation(location);

            try
            {
                anfAccount = await(Creation.createANFAccount(anfClient, resourceGroupName, anfAccountName, newAccount));
            }
            catch (CloudException e)
            {
                Utils.writeConsoleMessage("An error occurred while creating account: " + e.body().message());
                throw e;
            }
        }
        else
        {
            Utils.writeConsoleMessage("Account already exists");
        }

        //---------------------------
        // Create Capacity Pool
        //---------------------------
        Utils.writeConsoleMessage("Creating Source Capacity Pool at Premium service level...");

        String[] poolParamsSource = {resourceGroupName, anfAccountName, capacityPoolNameSource};
        CapacityPoolInner capacityPoolSource = await(CommonSdk.getResourceAsync(anfClient, poolParamsSource, CapacityPoolInner.class));
        if (capacityPoolSource == null)
        {
            CapacityPoolInner newCapacityPool = new CapacityPoolInner();
            newCapacityPool.withServiceLevel(ServiceLevel.fromString(capacityPoolServiceLevelSource));
            newCapacityPool.withSize(capacityPoolSize);
            newCapacityPool.withLocation(location);

            try
            {
                capacityPoolSource = await(Creation.createCapacityPool(anfClient, resourceGroupName, anfAccountName, capacityPoolNameSource, newCapacityPool));
            }
            catch (CloudException e)
            {
                Utils.writeConsoleMessage("An error occurred while creating source capacity pool: " + e.body().message());
                throw e;
            }
        }
        else
        {
            Utils.writeConsoleMessage("Source Capacity Pool already exists");
        }

        Utils.writeConsoleMessage("Creating Destination Capacity Pool at Standard service level...");

        String[] poolParamsDestination = {resourceGroupName, anfAccountName, capacityPoolNameDestination};
        CapacityPoolInner capacityPoolDestination = await(CommonSdk.getResourceAsync(anfClient, poolParamsDestination, CapacityPoolInner.class));
        if (capacityPoolDestination == null)
        {
            CapacityPoolInner newCapacityPool = new CapacityPoolInner();
            newCapacityPool.withServiceLevel(ServiceLevel.fromString(capacityPoolServiceLevelDestination));
            newCapacityPool.withSize(capacityPoolSize);
            newCapacityPool.withLocation(location);

            try
            {
                capacityPoolDestination = await(Creation.createCapacityPool(anfClient, resourceGroupName, anfAccountName, capacityPoolNameDestination, newCapacityPool));
            }
            catch (CloudException e)
            {
                Utils.writeConsoleMessage("An error occurred while creating destination capacity pool: " + e.body().message());
                throw e;
            }
        }
        else
        {
            Utils.writeConsoleMessage("Destination Capacity Pool already exists");
        }

        //---------------------------
        // Create Volume
        //---------------------------
        Utils.writeConsoleMessage("Creating Volume at Premium service level...");

        String[] volumeParams = {resourceGroupName, anfAccountName, capacityPoolNameSource, volumeName};
        VolumeInner volume = await(CommonSdk.getResourceAsync(anfClient, volumeParams, VolumeInner.class));
        if (volume == null)
        {
            String subnetId = "/subscriptions/" + subscriptionId + "/resourceGroups/" + resourceGroupName +
                    "/providers/Microsoft.Network/virtualNetworks/" + vnetName + "/subnets/" + subnetName;


            VolumeInner newVolume = new VolumeInner();
            newVolume.withLocation(location);
            newVolume.withServiceLevel(ServiceLevel.fromString(capacityPoolServiceLevelSource));
            newVolume.withCreationToken(volumeName);
            newVolume.withSubnetId(subnetId);
            newVolume.withUsageThreshold(volumeSize);
            newVolume.withProtocolTypes(Collections.singletonList("NFSv3"));

            try
            {
                volume = await(Creation.createVolume(anfClient, resourceGroupName, anfAccountName, capacityPoolNameSource, volumeName, newVolume));
            }
            catch (CloudException e)
            {
                Utils.writeConsoleMessage("An error occurred while creating volume: " + e.body().message());
                throw e;
            }
        }
        else
        {
            Utils.writeConsoleMessage("Volume already exists");
        }

        Utils.writeConsoleMessage("Current Volume service level: " + volume.serviceLevel());

        //---------------------------
        // Update Volume
        //---------------------------
        Utils.writeConsoleMessage("Performing Pool change. Updating Volume...");

        try
        {
            await(Update.volumePoolChange(anfClient, resourceGroupName, anfAccountName, capacityPoolNameSource, volumeName, capacityPoolDestination.id()));
            Utils.writeSuccessMessage("Pool change successful. Moved Volume from " + capacityPoolNameSource + " to " + capacityPoolNameDestination);
        }
        catch (CloudException e)
        {
            Utils.writeConsoleMessage("An error occurred while performing pool change: " + e.body().message());
            throw e;
        }

        volumeParams = new String[]{resourceGroupName, anfAccountName, capacityPoolNameDestination, volumeName};
        volume = await(CommonSdk.getResourceAsync(anfClient, volumeParams, VolumeInner.class));
        Utils.writeConsoleMessage("Current Volume service level: " + volume.serviceLevel());

        //---------------------------
        // Cleaning up resources
        //---------------------------

        /*
          Cleanup process. For this process to take effect please change the value of
          the boolean variable 'cleanup' to 'true'
          The cleanup process starts from the innermost resources down in the hierarchy chain.
          In this case: Volume -> Capacity Pool -> Account
        */
        if (cleanup)
        {
            Utils.writeConsoleMessage("Cleaning up all created resources");

            try
            {
                await(Cleanup.runCleanupTask(anfClient, volumeParams, VolumeInner.class));
                // ARM workaround to wait for the deletion to complete
                CommonSdk.waitForNoANFResource(anfClient, volume.id(), VolumeInner.class);
                Utils.writeSuccessMessage("Volume successfully deleted: " + volume.id());

                await(Cleanup.runCleanupTask(anfClient, poolParamsSource, CapacityPoolInner.class));
                CommonSdk.waitForNoANFResource(anfClient, capacityPoolSource.id(), CapacityPoolInner.class);
                Utils.writeSuccessMessage("Source Capacity Pool successfully deleted: " + capacityPoolSource.id());

                await(Cleanup.runCleanupTask(anfClient, poolParamsDestination, CapacityPoolInner.class));
                CommonSdk.waitForNoANFResource(anfClient, capacityPoolDestination.id(), CapacityPoolInner.class);
                Utils.writeSuccessMessage("Destination Capacity Pool successfully deleted: " + capacityPoolDestination.id());

                await(Cleanup.runCleanupTask(anfClient, accountParams, NetAppAccountInner.class));
                CommonSdk.waitForNoANFResource(anfClient, anfAccount.id(), NetAppAccountInner.class);
                Utils.writeSuccessMessage("Account successfully deleted: " + anfAccount.id());
            }
            catch (CloudException e)
            {
                Utils.writeConsoleMessage("An error occurred while deleting resource: " + e.body().message());
                throw e;
            }
        }


        return CompletableFuture.completedFuture(null);
    }
}
