/*
 *
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.apimgt.core.dao.impl;

import org.apache.commons.io.IOUtils;
import org.wso2.carbon.apimgt.core.exception.APIMgtDAOException;
import org.wso2.carbon.apimgt.core.models.DocumentInfo;
import org.wso2.carbon.apimgt.core.util.APIMgtConstants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides data access to API Docs data related tables
 */
class ApiDocDAO {
    private static final String AM_API_DOCS = "AM_API_DOCS";

    static List<DocumentInfo> getDocumentInfoList(Connection connection, String apiID) throws SQLException {
        final String query = "SELECT UUID, NAME, SUMMARY, TYPE, OTHER_TYPE_NAME, FILE_NAME, SOURCE_TYPE, " +
                "VISIBILITY, CONTENT FROM AM_API_DOCS WHERE API_ID = ?";

        List<DocumentInfo> metaDataList = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, apiID);
            statement.execute();

            try (ResultSet rs =  statement.getResultSet()) {
                while (rs.next()) {
                    metaDataList.add(new DocumentInfo.Builder().
                            id(rs.getString("UUID")).
                            name(rs.getString("NAME")).
                            summary(rs.getString("SUMMARY")).
                            type(DocumentInfo.DocType.valueOf(rs.getString("TYPE"))).
                            otherType(rs.getString("OTHER_TYPE_NAME")).
                            content(rs.getString("CONTENT")).
                            fileName(rs.getString("FILE_NAME")).
                            sourceType(DocumentInfo.SourceType.valueOf(rs.getString("SOURCE_TYPE"))).
                            visibility(DocumentInfo.Visibility.valueOf(rs.getString("VISIBILITY"))).build());
                }
            }
        }

        return metaDataList;
    }

    /**
     * Update doc info of documents
     *
     * @param connection   DB connection
     * @param documentInfo document info
     * @param updatedBy    user who performs the action
     * @throws SQLException
     */
    static void updateDocInfo(Connection connection, DocumentInfo documentInfo, String updatedBy) throws SQLException {
        deleteDOCPermission(connection, documentInfo.getId());
        addDOCPermission(connection, documentInfo.getPermissionMap(), documentInfo.getId());
        final String query = "UPDATE AM_API_DOCS SET NAME = ?, SUMMARY = ?, TYPE = ?, "
                + "OTHER_TYPE_NAME = ?, CONTENT = ?, FILE_NAME = ?, SOURCE_TYPE = ?, VISIBILITY = ?, "
                + "UPDATED_BY = ?, LAST_UPDATED_TIME = ? WHERE UUID = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, documentInfo.getName());
            statement.setString(2, documentInfo.getSummary());
            statement.setString(3, documentInfo.getType().toString());
            statement.setString(4, documentInfo.getOtherType());
            statement.setString(5, documentInfo.getContent());
            statement.setString(6, documentInfo.getFileName());
            statement.setString(7, documentInfo.getSourceType().toString());
            statement.setString(8, documentInfo.getVisibility().toString());
            statement.setString(9, updatedBy);
            statement.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(11, documentInfo.getId());
            statement.execute();
        }
    }

    /**
     * Get doc info of documents with source type URL or inline
     *
     * @param connection   DB connection
     * @param docID document ID
     * @throws SQLException
     */
    static DocumentInfo getDocumentInfo(Connection connection, String docID) throws SQLException {
        final String query = "SELECT AM_API_DOCS.UUID, AM_API_DOCS.NAME, AM_API_DOCS" +
                ".SUMMARY, AM_API_DOCS.TYPE, AM_API_DOCS.OTHER_TYPE_NAME, AM_API_DOCS" +
                ".CONTENT, AM_API_DOCS.FILE_NAME, AM_API_DOCS.SOURCE_TYPE, AM_API_DOCS" +
                ".VISIBILITY FROM AM_API_DOCS WHERE AM_API_DOCS.UUID = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, docID);
            statement.execute();

            try (ResultSet rs =  statement.getResultSet()) {
                while (rs.next()) {
                    return new DocumentInfo.Builder().
                            id(rs.getString("UUID")).
                            name(rs.getString("NAME")).
                            summary(rs.getString("SUMMARY")).
                            type(DocumentInfo.DocType.valueOf(rs.getString("TYPE"))).
                            otherType(rs.getString("OTHER_TYPE_NAME")).
                            content(rs.getString("CONTENT")).
                            sourceType(DocumentInfo.SourceType.valueOf(rs.getString("SOURCE_TYPE"))).
                            visibility(DocumentInfo.Visibility.valueOf(rs.getString("VISIBILITY"))).
                            fileName(rs.getString("FILE_NAME")).build();
                }
            }
        }

        return null;
    }

    /**
     * add doc info of documents with source type URL or inline
     *
     * @param connection   DB connection
     * @param documentInfo document Info
     * @throws SQLException
     */
    static void addDocumentInfo(Connection connection, DocumentInfo documentInfo, String apiID) throws SQLException {
        final String query = "INSERT INTO AM_API_DOCS (UUID, API_ID, NAME, SUMMARY, TYPE, OTHER_TYPE_NAME, " +
                "CONTENT, FILE_NAME, SOURCE_TYPE, VISIBILITY, CREATED_BY, CREATED_TIME, UPDATED_BY, " +
                "LAST_UPDATED_TIME) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, documentInfo.getId());
            statement.setString(2, apiID);
            statement.setString(3, documentInfo.getName());
            statement.setString(4, documentInfo.getSummary());
            statement.setString(5, documentInfo.getType().toString());
            statement.setString(6, documentInfo.getOtherType());
            statement.setString(7, documentInfo.getContent());
            statement.setString(8, documentInfo.getFileName());
            statement.setString(9, documentInfo.getSourceType().toString());
            statement.setString(10, documentInfo.getVisibility().toString());
            statement.setString(11, documentInfo.getCreatedBy());
            statement.setTimestamp(12, Timestamp.from(documentInfo.getCreatedTime()));
            statement.setString(13, documentInfo.getUpdatedBy());
            statement.setTimestamp(14, Timestamp.from(documentInfo.getLastUpdatedTime()));
            statement.execute();
            addDOCPermission(connection, documentInfo.getPermissionMap(), documentInfo.getId());
        }

    }

    static String getLastUpdatedTimeOfDocument(String documentId) throws APIMgtDAOException {
        return EntityDAO.getLastUpdatedTimeOfResourceByUUID(AM_API_DOCS, documentId);
    }

    private static void deleteDOCPermission(Connection connection, String docID) throws SQLException {
        final String query = "DELETE FROM AM_DOC_GROUP_PERMISSION WHERE DOC_ID = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, docID);
            statement.execute();
        }
    }

    /**
     * Add DOC permission
     * @param connection connection
     * @param permissionMap  permission map
     * @param docId document Id.
     * @throws SQLException
     */
    private static void addDOCPermission(Connection connection, HashMap permissionMap, String docId) throws
            SQLException {
        final String query = "INSERT INTO AM_DOC_GROUP_PERMISSION (DOC_ID, GROUP_ID, PERMISSION) VALUES (?, ?, ?)";
        Map<String, Integer> map = permissionMap;
        if (permissionMap != null) {
            if (permissionMap.size() > 0) {
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    for (Map.Entry<String, Integer> entry : map.entrySet()) {
                        statement.setString(1, docId);
                        statement.setString(2, entry.getKey());
                        //if permission value is UPDATE or DELETE we by default give them read permission also.
                        if (entry.getValue() < APIMgtConstants.Permission.READ_PERMISSION && entry.getValue() != 0) {
                            statement.setInt(3, entry.getValue() + APIMgtConstants.Permission.READ_PERMISSION);
                        } else {
                            statement.setInt(3, entry.getValue());
                        }
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
            }
        } else {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, docId);
                statement.setString(2, APIMgtConstants.Permission.EVERYONE_GROUP);
                statement.setInt(3, 7);
                statement.execute();
            }
        }

    }

    /**
     * Update the binary content of a given API documentation
     * @param connection connection
     * @param docID document Id
     * @param resourceValue document input stream
     * @throws SQLException
     */
    static int updateBinaryDocContent(Connection connection, String docID, InputStream resourceValue,
                                      String updatedBy) throws SQLException {
        final String query = "UPDATE AM_API_DOCS SET CONTENT_BINARY_VALUE = ?, UPDATED_BY = ?, "
                + "LAST_UPDATED_TIME = ? WHERE UUID = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setBinaryStream(1, resourceValue);
            statement.setString(2, updatedBy);
            statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(4, docID);
            return statement.executeUpdate();
        }
    }

    /**
     * Get the binary content of a given API documentation
     * @param connection connection
     * @param docID document Id
     * @throws SQLException
     */
    static InputStream getBinaryDocContent(Connection connection, String docID) throws SQLException, IOException {
        final String query = "SELECT CONTENT_BINARY_VALUE FROM AM_API_RESOURCES WHERE UUID = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, docID);
            statement.execute();

            try (ResultSet rs = statement.getResultSet()) {
                if (rs.next()) {
                    InputStream inputStream = rs.getBinaryStream("CONTENT_BINARY_VALUE");
                    if (inputStream != null) {
                        return new ByteArrayInputStream(IOUtils.toByteArray(inputStream));
                    }
                }
            }
        }
        return null;
    }

    /**
     * Delete given API documentation
     * @param connection connection
     * @param docID document Id
     * @throws SQLException
     */
    static void deleteDocument(Connection connection, String docID)
            throws SQLException {
        final String query = "DELETE FROM DOCS WHERE UUID = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, docID);
            statement.execute();
        }
    }
}
