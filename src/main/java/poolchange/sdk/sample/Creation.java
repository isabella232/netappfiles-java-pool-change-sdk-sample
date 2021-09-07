// Copyright (c) Microsoft and contributors.  All rights reserved.
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package poolchange.sdk.sample;

import com.azure.resourcemanager.netapp.fluent.NetAppManagementClient;
import com.azure.resourcemanager.netapp.fluent.models.CapacityPoolInner;
import com.azure.resourcemanager.netapp.fluent.models.NetAppAccountInner;
import com.azure.resourcemanager.netapp.fluent.models.VolumeInner;
import poolchange.sdk.sample.common.Utils;

public class Creation
{
    /**
     * Creates an ANF Account
     * @param anfClient Azure NetApp Files Management Client
     * @param resourceGroup Name of the resource group where the Account will be created
     * @param accountName Name of the Account being created
     * @param accountBody The Account body used in the creation
     * @return The newly created ANF Account
     */
    public static NetAppAccountInner createANFAccount(NetAppManagementClient anfClient, String resourceGroup,
                                                                         String accountName, NetAppAccountInner accountBody)
    {
        NetAppAccountInner anfAccount = anfClient.getAccounts().beginCreateOrUpdate(resourceGroup, accountName, accountBody).getFinalResult();
        Utils.writeSuccessMessage("Account successfully created, resourceId: " + anfAccount.id());

        return anfAccount;
    }

    /**
     * Creates a Capacity Pool
     * @param anfClient Azure NetApp Files Management Client
     * @param resourceGroup Name of the resource group where the Capacity Pool will be created
     * @param accountName Name of the Account
     * @param poolName Name of the Capacity Pool being created
     * @param poolBody The Capacity Pool body used in the creation
     * @return The newly created Capacity Pool
     */
    public static CapacityPoolInner createCapacityPool(NetAppManagementClient anfClient, String resourceGroup,
                                                                          String accountName, String poolName, CapacityPoolInner poolBody)
    {
        CapacityPoolInner capacityPool = anfClient.getPools().beginCreateOrUpdate(resourceGroup, accountName, poolName, poolBody).getFinalResult();
        Utils.writeSuccessMessage("Capacity Pool successfully created, resourceId: " + capacityPool.id());

        return capacityPool;
    }

    /**
     * Creates a Volume with an attached Snapshot Policy
     * @param anfClient Azure NetApp Files Management Client
     * @param resourceGroup Name of the resource group where the Volume will be created
     * @param accountName Name of the Account
     * @param poolName Name of the Capacity Pool
     * @param volumeName Name of the Volume being created
     * @param volumeBody The Volume body used in the creation
     * @return The newly created Volume
     */
    public static VolumeInner createVolume(NetAppManagementClient anfClient, String resourceGroup,
                                                              String accountName, String poolName, String volumeName, VolumeInner volumeBody)
    {
        VolumeInner volume = anfClient.getVolumes().beginCreateOrUpdate(resourceGroup, accountName, poolName, volumeName, volumeBody).getFinalResult();
        Utils.writeSuccessMessage("Volume successfully created, resourceId: " + volume.id());

        return volume;
    }
}
