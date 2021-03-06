/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.content.search;

import java.lang.String;
import java.util.ArrayList;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;

import org.apache.lucene.util.Version;

/**
 * SearchWorker Class
 */
public class SearchWorker {

    public static final String module = SearchWorker.class.getName();

    public static final Version LUCENE_VERSION = Version.LUCENE_4_9;

    public static void indexContentTree(LocalDispatcher dispatcher, Delegator delegator, String siteId) throws Exception {
        GenericValue content = delegator.makeValue("Content", UtilMisc.toMap("contentId", siteId));
        List<GenericValue> siteList = ContentWorker.getAssociatedContent(content, "To", UtilMisc.toList("SUBSITE", "PUBLISH_LINK", "SUB_CONTENT"), null, UtilDateTime.nowTimestamp().toString(), null);

        if (siteList != null) {
            for (GenericValue siteContent : siteList) {
                String siteContentId = siteContent.getString("contentId");
                List<GenericValue> subContentList = ContentWorker.getAssociatedContent(siteContent, "To", UtilMisc.toList("SUBSITE", "PUBLISH_LINK", "SUB_CONTENT"), null, UtilDateTime.nowTimestamp().toString(), null);

                if (subContentList != null) {
                    List<String> contentIdList = new ArrayList<String>();
                    for (GenericValue subContent : subContentList) {
                        contentIdList.add(subContent.getString("contentId"));
                    }
                    indexContentList(dispatcher, delegator, contentIdList);
                    indexContentTree(dispatcher, delegator, siteContentId);
                }
            }
        }
    }

    public static String getIndexPath(String path) {
        String basePath = UtilProperties.getPropertyValue("search", "defaultIndex", "index");
        return (UtilValidate.isNotEmpty(path)? basePath + "/" + path: basePath);
    }

    public static void indexContentList(LocalDispatcher dispatcher, Delegator delegator, List<String> idList) throws Exception {
        DocumentIndexer indexer = DocumentIndexer.getInstance(delegator, "content");
        List<GenericValue> contentList = new ArrayList<GenericValue>();
        for (String id : idList) {
            try {
                GenericValue content = delegator.findOne("Content", UtilMisc .toMap("contentId", id), true);
                if (content != null) {
                    contentList.add(content);
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return;
            }
        }
        for (GenericValue content : contentList) {
            indexer.queue(new ContentDocument(content, dispatcher));
        }
    }
}
