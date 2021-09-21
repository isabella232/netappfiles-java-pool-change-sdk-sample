// Copyright (c) Microsoft and contributors.  All rights reserved.
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package poolchange.sdk.sample;

import com.azure.core.credential.TokenCredential;
import com.azure.core.exception.AzureException;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.netapp.NetAppFilesManager;
import com.azure.resourcemanager.netapp.fluent.models.CapacityPoolInner;
import com.azure.resourcemanager.netapp.fluent.models.NetAppAccountInner;
import com.azure.resourcemanager.netapp.fluent.models.VolumeInner;
import com.azure.resourcemanager.netapp.models.ServiceLevel;
import poolchange.sdk.sample.common.CommonSdk;
import poolchange.sdk.sample.common.Utils;

import java.util.Collections;

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
            run();
            Utils.writeConsoleMessage("Sample application successfully completed execution");
        }
        catch (Exception e)
        {
            Utils.writeErrorMessage(e.getMessage());
        }

        System.exit(0);
    }

    private static void run()
    {
        //---------------------------------------------------------------------------------------------------------------------
        // Setting variables necessary for resources creation - change these to appropriate values related to your environment
        //---------------------------------------------------------------------------------------------------------------------
        boolean cleanup = false;

        String subscriptionId = "<subscription-id>";
        String location = "<location>";
        String resourceGroupName = "<resource-group-name>";
        String vnetName = "<vnet-name>";
        String subnetName = "<subnet-name>";
        String anfAccountName = "anf-java-example-account";
        String capacityPoolNameSource = "anf-java-example-pool-source";
        String capacityPoolNameDestination = "anf-java-example-pool-destination";
        String capacityPoolServiceLevelSource = "Premium"; // Valid service levels are: Ultra, Premium, Standard
        String capacityPoolServiceLevelDestination = "Standard";
        String volumeName = "anf-java-example-volume";

        long capacityPoolSize = 4398046511104L;  // 4TiB which is minimum size
        long volumeSize = 107374182400L;  // 100GiB - volume minimum size

        // Instantiating a new ANF management client and authenticate
        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
        TokenCredential credential = new DefaultAzureCredentialBuilder()
                .authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint())
                .build();
        Utils.writeConsoleMessage("Instantiating a new Azure NetApp Files management client...");
        NetAppFilesManager manager = NetAppFilesManager
                .authenticate(credential, profile);


        //---------------------------
        // Creating ANF resources
        //---------------------------

        //---------------------------
        // Create ANF Account
        //---------------------------
        Utils.writeConsoleMessage("Creating Azure NetApp Files Account...");

        String[] accountParams = {resourceGroupName, anfAccountName};
        NetAppAccountInner anfAccount = (NetAppAccountInner) CommonSdk.getResource(manager.serviceClient(), accountParams, NetAppAccountInner.class);
        if (anfAccount == null)
        {
            NetAppAccountInner newAccount = new NetAppAccountInner();
            newAccount.withLocation(location);

            try
            {
                anfAccount = Creation.createANFAccount(manager.serviceClient(), resourceGroupName, anfAccountName, newAccount);
            }
            catch (AzureException e)
            {
                Utils.writeConsoleMessage("An error occurred while creating account: " + e.getMessage());
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
        CapacityPoolInner capacityPoolSource = (CapacityPoolInner) CommonSdk.getResource(manager.serviceClient(), poolParamsSource, CapacityPoolInner.class);
        if (capacityPoolSource == null)
        {
            CapacityPoolInner newCapacityPool = new CapacityPoolInner();
            newCapacityPool.withServiceLevel(ServiceLevel.fromString(capacityPoolServiceLevelSource));
            newCapacityPool.withSize(capacityPoolSize);
            newCapacityPool.withLocation(location);

            try
            {
                capacityPoolSource = Creation.createCapacityPool(manager.serviceClient(), resourceGroupName, anfAccountName, capacityPoolNameSource, newCapacityPool);
            }
            catch (AzureException e)
            {
                Utils.writeConsoleMessage("An error occurred while creating source capacity pool: " + e.getMessage());
                throw e;
            }
        }
        else
        {
            Utils.writeConsoleMessage("Source Capacity Pool already exists");
        }

        Utils.writeConsoleMessage("Creating Destination Capacity Pool at Standard service level...");

        String[] poolParamsDestination = {resourceGroupName, anfAccountName, capacityPoolNameDestination};
        CapacityPoolInner capacityPoolDestination = (CapacityPoolInner) CommonSdk.getResource(manager.serviceClient(), poolParamsDestination, CapacityPoolInner.class);
        if (capacityPoolDestination == null)
        {
            CapacityPoolInner newCapacityPool = new CapacityPoolInner();
            newCapacityPool.withServiceLevel(ServiceLevel.fromString(capacityPoolServiceLevelDestination));
            newCapacityPool.withSize(capacityPoolSize);
            newCapacityPool.withLocation(location);

            try
            {
                capacityPoolDestination = Creation.createCapacityPool(manager.serviceClient(), resourceGroupName, anfAccountName, capacityPoolNameDestination, newCapacityPool);
            }
            catch (AzureException e)
            {
                Utils.writeConsoleMessage("An error occurred while creating destination capacity pool: " + e.getMessage());
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
        VolumeInner volume = (VolumeInner) CommonSdk.getResource(manager.serviceClient(), volumeParams, VolumeInner.class);
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
                volume = Creation.createVolume(manager.serviceClient(), resourceGroupName, anfAccountName, capacityPoolNameSource, volumeName, newVolume);
                Utils.writeSuccessMessage("Volume successfully created, resource id: " + volume.id());
            }
            catch (AzureException e)
            {
                Utils.writeConsoleMessage("An error occurred while creating volume: " + e.getMessage());
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
            Update.volumePoolChange(manager.serviceClient(), resourceGroupName, anfAccountName, capacityPoolNameSource, volumeName, capacityPoolDestination.id());
            Utils.writeSuccessMessage("Pool change successful. Moved Volume from " + capacityPoolNameSource + " to " + capacityPoolNameDestination);
        }
        catch (AzureException e)
        {
            Utils.writeConsoleMessage("An error occurred while performing pool change: " + e.getMessage());
            throw e;
        }

        volumeParams = new String[]{resourceGroupName, anfAccountName, capacityPoolNameDestination, volumeName};
        volume = (VolumeInner) CommonSdk.getResource(manager.serviceClient(), volumeParams, VolumeInner.class);
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
                Cleanup.runCleanupTask(manager.serviceClient(), volumeParams, VolumeInner.class);
                // ARM workaround to wait for the deletion to complete
                CommonSdk.waitForNoANFResource(manager.serviceClient(), volume.id(), VolumeInner.class);
                Utils.writeSuccessMessage("Volume successfully deleted: " + volume.id());

                Cleanup.runCleanupTask(manager.serviceClient(), poolParamsSource, CapacityPoolInner.class);
                CommonSdk.waitForNoANFResource(manager.serviceClient(), capacityPoolSource.id(), CapacityPoolInner.class);
                Utils.writeSuccessMessage("Source Capacity Pool successfully deleted: " + capacityPoolSource.id());

                Cleanup.runCleanupTask(manager.serviceClient(), poolParamsDestination, CapacityPoolInner.class);
                CommonSdk.waitForNoANFResource(manager.serviceClient(), capacityPoolDestination.id(), CapacityPoolInner.class);
                Utils.writeSuccessMessage("Destination Capacity Pool successfully deleted: " + capacityPoolDestination.id());

                Cleanup.runCleanupTask(manager.serviceClient(), accountParams, NetAppAccountInner.class);
                CommonSdk.waitForNoANFResource(manager.serviceClient(), anfAccount.id(), NetAppAccountInner.class);
                Utils.writeSuccessMessage("Account successfully deleted: " + anfAccount.id());
            }
            catch (AzureException e)
            {
                Utils.writeConsoleMessage("An error occurred while deleting resource: " + e.getMessage());
                throw e;
            }
        }
    }
}
