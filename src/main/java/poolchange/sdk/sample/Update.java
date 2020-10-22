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
    public static CompletableFuture<Void> volumePoolChange(AzureNetAppFilesManagementClientImpl anfClient, String resourceGroupName,
                                                           String accountName, String poolName, String volumeName, String newPoolResourceId)
    {
        anfClient.volumes().poolChange(resourceGroupName, accountName, poolName, volumeName, newPoolResourceId);

        return CompletableFuture.completedFuture(null);
    }
}
