// Copyright (c) Microsoft and contributors.  All rights reserved.
//
// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

package poolchange.sdk.sample;

import com.microsoft.azure.management.netapp.v2020_06_01.implementation.AzureNetAppFilesManagementClientImpl;
import poolchange.sdk.sample.common.Utils;

import java.util.concurrent.CompletableFuture;

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
     * @return
     */
    public static CompletableFuture<Void> volumePoolChange(AzureNetAppFilesManagementClientImpl anfClient, String resourceGroupName,
                                                           String accountName, String poolName, String volumeName, String newPoolResourceId)
    {
        anfClient.volumes().poolChange(resourceGroupName, accountName, poolName, volumeName, newPoolResourceId);

        return CompletableFuture.completedFuture(null);
    }
}
