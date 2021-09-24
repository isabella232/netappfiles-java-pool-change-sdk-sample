// Copyright (c) Microsoft and contributors.  All rights reserved.
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package poolchange.sdk.sample;

import com.azure.resourcemanager.netapp.fluent.NetAppManagementClient;
import com.azure.resourcemanager.netapp.models.PoolChangeRequest;

public class Update
{
    /**
     * Perform pool change on Volume
     * @param anfClient Azure NetApp Files Management Client
     * @param resourceGroupName Name of the resource group
     * @param accountName Name of the Account
     * @param poolName Name of Volume's current Capacity Pool
     * @param volumeName Name of the Volume being updated
     * @param newPoolResourceId Resource id of new capacity pool
     */
    public static void volumePoolChange(NetAppManagementClient anfClient, String resourceGroupName,
                                                           String accountName, String poolName, String volumeName, String newPoolResourceId)
    {
        PoolChangeRequest request = new PoolChangeRequest();
        request.withNewPoolResourceId(newPoolResourceId);
        anfClient.getVolumes().beginPoolChange(resourceGroupName, accountName, poolName, volumeName, request).getFinalResult();
    }
}
