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
        String capacityPoolNamePrimary = "pool-primary";
        String capacityPoolNameSecondary = "pool-secondary";
        String capacityPoolServiceLevelPrimary = "Premium"; // Valid service levels are: Ultra, Premium, Standard
        String capacityPoolServiceLevelSecondary = "Standard";
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
        Utils.writeConsoleMessage("Creating Primary Capacity Pool at Premium service level...");

        String[] poolParamsPrimary = {resourceGroupName, anfAccountName, capacityPoolNamePrimary};
        CapacityPoolInner capacityPoolPrimary = await(CommonSdk.getResourceAsync(anfClient, poolParamsPrimary, CapacityPoolInner.class));
        if (capacityPoolPrimary == null)
        {
            CapacityPoolInner newCapacityPool = new CapacityPoolInner();
            newCapacityPool.withServiceLevel(ServiceLevel.fromString(capacityPoolServiceLevelPrimary));
            newCapacityPool.withSize(capacityPoolSize);
            newCapacityPool.withLocation(location);

            try
            {
                capacityPoolPrimary = await(Creation.createCapacityPool(anfClient, resourceGroupName, anfAccountName, capacityPoolNamePrimary, newCapacityPool));
            }
            catch (CloudException e)
            {
                Utils.writeConsoleMessage("An error occurred while creating primary capacity pool: " + e.body().message());
                throw e;
            }
        }
        else
        {
            Utils.writeConsoleMessage("Primary Capacity Pool already exists");
        }

        Utils.writeConsoleMessage("Creating Secondary Capacity Pool at Standard service level...");

        String[] poolParamsSecondary = {resourceGroupName, anfAccountName, capacityPoolNameSecondary};
        CapacityPoolInner capacityPoolSecondary = await(CommonSdk.getResourceAsync(anfClient, poolParamsSecondary, CapacityPoolInner.class));
        if (capacityPoolSecondary == null)
        {
            CapacityPoolInner newCapacityPool = new CapacityPoolInner();
            newCapacityPool.withServiceLevel(ServiceLevel.fromString(capacityPoolServiceLevelSecondary));
            newCapacityPool.withSize(capacityPoolSize);
            newCapacityPool.withLocation(location);

            try
            {
                capacityPoolSecondary = await(Creation.createCapacityPool(anfClient, resourceGroupName, anfAccountName, capacityPoolNameSecondary, newCapacityPool));
            }
            catch (CloudException e)
            {
                Utils.writeConsoleMessage("An error occurred while creating secondary capacity pool: " + e.body().message());
                throw e;
            }
        }
        else
        {
            Utils.writeConsoleMessage("Secondary Capacity Pool already exists");
        }

        //---------------------------
        // Create Volume
        //---------------------------
        Utils.writeConsoleMessage("Creating Volume at Premium service level...");

        String[] volumeParams = {resourceGroupName, anfAccountName, capacityPoolNamePrimary, volumeName};
        VolumeInner volume = await(CommonSdk.getResourceAsync(anfClient, volumeParams, VolumeInner.class));
        if (volume == null)
        {
            String subnetId = "/subscriptions/" + subscriptionId + "/resourceGroups/" + resourceGroupName +
                    "/providers/Microsoft.Network/virtualNetworks/" + vnetName + "/subnets/" + subnetName;


            VolumeInner newVolume = new VolumeInner();
            newVolume.withLocation(location);
            newVolume.withServiceLevel(ServiceLevel.fromString(capacityPoolServiceLevelPrimary));
            newVolume.withCreationToken(volumeName);
            newVolume.withSubnetId(subnetId);
            newVolume.withUsageThreshold(volumeSize);
            newVolume.withProtocolTypes(Collections.singletonList("NFSv3"));

            try
            {
                volume = await(Creation.createVolume(anfClient, resourceGroupName, anfAccountName, capacityPoolNamePrimary, volumeName, newVolume));
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
            await(Update.volumePoolChange(anfClient, resourceGroupName, anfAccountName, capacityPoolNamePrimary, volumeName, capacityPoolSecondary.id()));
            Utils.writeSuccessMessage("Pool change successful. Moved Volume from " + capacityPoolNamePrimary + " to " + capacityPoolNameSecondary);
        }
        catch (CloudException e)
        {
            Utils.writeConsoleMessage("An error occurred while performing pool change: " + e.body().message());
            throw e;
        }

        volumeParams = new String[]{resourceGroupName, anfAccountName, capacityPoolNameSecondary, volumeName};
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

                await(Cleanup.runCleanupTask(anfClient, poolParamsPrimary, CapacityPoolInner.class));
                CommonSdk.waitForNoANFResource(anfClient, capacityPoolPrimary.id(), CapacityPoolInner.class);
                Utils.writeSuccessMessage("Primary Capacity Pool successfully deleted: " + capacityPoolPrimary.id());

                await(Cleanup.runCleanupTask(anfClient, poolParamsSecondary, CapacityPoolInner.class));
                CommonSdk.waitForNoANFResource(anfClient, capacityPoolSecondary.id(), CapacityPoolInner.class);
                Utils.writeSuccessMessage("Secondary Capacity Pool successfully deleted: " + capacityPoolSecondary.id());

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
