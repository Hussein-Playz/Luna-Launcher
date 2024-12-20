/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.luna.viewmodel.impl;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy.FetchStrategy;
import com.apollographql.apollo.exception.ApolloException;
import com.luna.graphql.GetNewsQuery;
import com.luna.managers.ConfigManager;
import com.luna.managers.LogManager;
import com.luna.managers.NewsManager;
import com.luna.network.GraphqlClient;
import com.luna.viewmodel.base.INewsViewModel;

public class NewsViewModel implements INewsViewModel {
    private Consumer<String> _onReload;

    @Override
    public void addOnReloadListener(Consumer<String> onReload) {
        _onReload = onReload;
    }

    @Override
    public void reload() {
        // Ignore reloads if nothing is waiting for the content
        if (_onReload == null) return;

        if (ConfigManager.getConfigItem("useGraphql.news", false)) {
            GraphqlClient.apolloClient.query(new GetNewsQuery(10))
                .toBuilder()
                .httpCachePolicy(new HttpCachePolicy.Policy(FetchStrategy.CACHE_FIRST, 30, TimeUnit.MINUTES, false))
                .build()
                .enqueue(new ApolloCall.Callback<GetNewsQuery.Data>() {
                    @Override
                    public void onResponse(@NotNull Response<GetNewsQuery.Data> response) {
                        _onReload.accept(NewsManager.getNewsHTML(response.getData().generalNews()));
                    }

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        LogManager.logStackTrace("Error fetching news", e);
                        _onReload.accept(NewsManager.getNewsHTML());
                    }
                });
        } else {
            _onReload.accept(NewsManager.getNewsHTML());
        }
    }
}
