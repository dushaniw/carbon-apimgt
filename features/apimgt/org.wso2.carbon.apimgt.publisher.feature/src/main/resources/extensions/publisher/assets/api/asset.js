/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
asset.manager = function(ctx) {
    var apiPublisher =  require('apipublisher').apipublisher;
    var LOGGED_IN_USER = 'LOGGED_IN_USER';
    var log = new Log('default-asset');

    //TODO Move to common module
    var isValiedImage = function(image){
        var allowedImageUploads = ["png","gif","jpg","jpeg"]; //default allowed image extentions.
        var skipImageMimeCheck = false;

        // put a config option to config.json
        var site = require('/extensions/assets/api/config/config.json');
        if(site.allowedImageUploads){
            allowedImageUploads = site.allowedImageUploads;
        }
        if(site.skipImageMimeCheck){
            skipImageMimeCheck = site.skipImageMimeCheck;
        }

        var name = image.getName();
        var extention = name.match( /\.[0-9a-z]{1,5}$/im )[0];
        extention = extention.replace(".","");


        if(allowedImageUploads.indexOf(extention) == -1){
            return false;
        }

        var contentType = image.getContentType();
        // following check was put in bcos if the mimetype was refered by the backend.
        if(!skipImageMimeCheck && !contentType.match(/^image/i)){
            return false;
        }

        // @todo magic number validation.
        // @todo validate with image manipulation. this would be the ultimate solution.

        return true;
    };

    var generate_swagger_object=function(swagger){
    swaggerObj = {
        api_doc : parse(swagger),
        resources : []
     };

    for(i = 0 ; i < swaggerObj.api_doc.apis.length ; i++ ){
        swaggerObj.resources.push(swaggerObj.api_doc.apis[i].file);
        delete swaggerObj.api_doc.apis[i].file
    }
    return stringify(swaggerObj);
    }
    return {
        importAssetFromHttpRequest: function(options) {
            var asset = {};
            var attributes = {};
            if (options.id) {
            asset.id = options.id;
            }
            asset.attributes=options;
            //asset.name = this._super.getName.call(asset);
            return asset;
        },
        create: function(options) {
            var result,obj,error,message,data;
            var api = {};
            var rxtModule = require('rxt');
            var assetMod = rxtModule.asset;
            if(options.attributes.action=="design"){
            api.apiName = options.attributes.overview_name;
            api.name = options.attributes.overview_name;
            api.version = options.attributes.overview_version;
            if (options.attributes.provider == null) {
                api.provider = ctx.username;
            } else {
                api.provider = options.attributes.overview_provider;
            }
            api.context = options.attributes.overview_context;

            //TODO now we no need to save Icon through API manager as asset API does it for us
            //Need to properly cope with that changed
            api.thumbnailContent = request.getFile("overview_thumbnail");
            api.thumbnailUrl = null;

            //validate uploaded image and set API has a image if content is valid
            if(api.thumbnailContent != null && isValiedImage(api.thumbnailContent)){
               api.thumbnailUrl = 'overview_thumbnail';
            } else if(api.thumbnailContent != null && !isValiedImage(api.thumbnailContent)){
                obj = {
                    error:true,
                    message:"Please upload a valid image file for the API icon."
                };
                print(obj);
                return;
            }

            //If API not exist create
            var apiProxy = apiPublisher.instance(ctx.username);
            result=apiProxy.checkIfAPIExists(api.provider,api.name,api.version);

            if(!result){
                result = apiProxy.designAPI(api);
                if (result!=null && result.error) {
                    throw "Error while creating the API.";
                }
                else{
                options.id=result;
                options.name=api.name;
                options.attributes.overview_provider=api.provider;
                options.attributes.overview_status='CREATED';
                }
            }
            api.description = options.attributes.overview_description;
            api.tags = options.attributes.overview_tags;
            api.visibility = options.attributes.visibility;
            api.visibleRoles = options.attributes.roles;
            api.swagger = generate_swagger_object(options.attributes.swagger);
            result = apiProxy.updateDesignAPI(api);
            if (result!=null && result.error) {
            throw "Error while updating the API.";
            }
            }


        },
        remove : function(id) {
            var asset = this.get.call(this, id);
           // log.debug("Removing API of Id " +id+ "Name " + asset.attributes.overview_name);
            var apiProxy = apiPublisher.instance(ctx.username);
            var result;
            try{
            result=apiProxy.deleteAPI(asset.attributes.overview_provider, asset.attributes.overview_name, asset.version);
            return result;
            } catch (e){
            log.error("Error while deleting the API-"+asset.attributes.overview_name+"-"+asset.version);
            throw e;
            }
        },
        list: function(paging) {
            //log.info(this._super.list.call(this, paging));
            return this._super.list.call(this, paging);
        },
        update: function(options){
            if(options.attributes.action == "implement"){
                var result, obj;
                var api = {};
                api.apiName = options.attributes.overview_name;
                api.version = options.attributes.overview_version;
                api.provider= options.attributes.overview_provider;
                var apiId = {
                    apiName : options.attributes.overview_name,
                    version : options.attributes.overview_version,
                    provider: options.attributes.overview_provider
                };
                api.context = options.attributes.overview_context;

              //api.implementation_type = options.attributes.implementation_methods;
                api.wsdl = options.attributes.wsdl;
                api.wadl = options.attributes.wadl;
                api.endpointSecured = options.attributes.endpointType;
                api.endpointUTUsername = options.attributes.epUsername;
                api.endpointUTPassword = options.attributes.epPassword;
                api.endpoint_config= options.attributes.endpoint_config;
                api.destinationStats= options.attributes.destinationStats;
                api.advertiseOnly= options.attributes.overview_advertiseOnly;
                //api.swagger = generate_swagger_object(options.attributes.swagger);

                var apiProxy = apiPublisher.instance(ctx.username);
                result = apiProxy.implementAPI(api);
                // log.info(result);
                if (result != null && result.error==true) {
                    obj = {
                        error:true,
                        message:result.message,
                        data :apiId,
                    };
                } else {
                    obj = {
                        error:false,
                        data :apiId
                    }
                }
               return obj;
            } else if(options.attributes.action == "manage") {
                var apiData = {};
                //log.info(options);
                apiData.apiName = options.name;
                apiData.version = options.attributes.version;
                if (request.getParameter("provider") == null) {
                    apiData.provider = ctx.username;
                } else {
                    apiData.provider = options.attributes.provider;
                }
                var apiId = {
                    apiName : apiData.apiName,
                    version : apiData.version,
                    provider: apiData.provider
                };

                apiData.context = options.attributes.overview_context;
                apiData.defaultVersion = options.attributes.default_version_checked;
                apiData.swagger = generate_swagger_object(options.attributes.swagger);
                apiData.tier = options.attributes.tiersCollection;
                if(options.attributes.transport_http == null && options.attributes.transport_https == null){
                    apiData.transports = null;
                }
                else if(options.attributes.transport_http != null && options.attributes.transport_https != null) {
                    apiData.transports=options.attributes.transport_http+","+options.attributes.transport_https;

                }else if(options.attributes.transport_http != null){
                    apiData.transports=options.attributes.transport_http;
                }else{
                    apiData.transports=options.attributes.transport_https;
                }
                if(options.attributes.inSequence == 'none') {
                    options.attributes.inSequence = null;
                }
                if(options.attributes.outSequence == 'none') {
                    options.attributes.outSequence = null;
                }if(options.attributes.faultSequence == 'none') {
                    options.attributes.faultSequence = null;
                }
                apiData.inSequence = options.attributes.inSequence;
                apiData.outSequence= options.attributes.outSequence;
                apiData.responseCache = options.attributes.responseCache;
                apiData.subscriptionAvailability = options.attributes.subscriptions;
                apiData.subscriptionTenants = options.attributes.tenants;
                apiData.bizOwner = options.attributes.bizOwner;
                apiData.bizOwnerMail = options.attributes.bizOwnerMail;
                apiData.techOwner = options.attributes.techOwner;
                apiData.techOwnerMail = options.attributes.techOwnerMail;
                apiData.faultSequence = options.attributes.faultSequence;
                apiData.cacheTimeout= options.attributes.cacheTimeout;
                apiData.destinationStats= options.attributes.destinationStatsEnabled;
                apiData.environments = options.attributes.environments;
                var apiProxy = apiPublisher.instance(ctx.username);
                result = apiProxy.manageAPI(apiData);
                if (result != null && result.error) {
                    log.error(result.message);
                    throw "Error while updating the API."
                } else {
                    obj = {
                        error: false,
                        data: apiId
                    }
                }
                return obj;
            }
        }
    };
};

asset.server = function (ctx) {
    return {
        endpoints: {
            pages: [{
                        title: 'Design an API',
                        url: 'design',
                        path: 'design.jag'
                    }, {
                        title: 'Implement an API',
                        url: 'implement',
                        path: 'implement.jag'
                    }, {
                        title: 'Manage an API',
                        url: 'manage',
                        path: 'manage.jag'
                    }, {
                        title: 'Documents',
                        url: 'documents',
                        path: 'documents.jag'
                    }, {
                        title: 'Tier Permissions',
                        url: 'tier_permissions',
                        path: 'tier_permissions.jag'
                    }, {
                        title: 'Manage',
                        url: 'manage',
                        path: 'manage.jag'
                    }],
            apis: [{
                       url: 'endpoints',
                       path: 'endpoints.jag'
                   }, {
                       url: 'prototype',
                       path: 'prototype.jag'
                   }, {
                       url: 'swagger',
                       path: 'swagger.jag'
                   }, {
                       url: 'lifecycle',
                       path: 'lifecycle.jag'
                   },{
                       url: 'apimanage',
                       path: 'apimanage.jag'
                   },{
                       url: 'sequences',
                       path: 'sequences.jag'
                   }, {
                       url: 'tiers',
                       path: 'tiers.jag'
                   }]
        }
    }
};
asset.configure = function (ctx) {
    return {
        table: {
            overview: {
                fields: {
                    thumbnail: {
                        type: 'file'
                    }
                }
            }
        },

        meta: {
            lifecycle: {
                name: 'APILifeCycle',
                commentRequired: false,
                defaultLifecycleEnabled:true,
                defaultAction: '',
                deletableStates: [],
                publishedStates: ['Published'],
                lifecycleEnabled: true
            },
            thumbnail: 'overview_thumbnail'
        }

    };
};
asset.renderer = function (ctx) {
    var type = ctx.assetType;
    var log = new Log();
    var listLinks = function (ribbon, utils) {
        ribbon.enabled = false;
        ribbon.list = [];
    };
    var apiLinks = function (ribbon, utils) {
        var navList = utils.navList();
        navList.push('Overview', 'icon-list-alt', utils.buildUrl('overview'));
        navList.push('Lifecycle', 'icon-list-alt', utils.buildUrl('lifecycle'));
        navList.push('Version', 'icon-list-alt', utils.buildUrl('version'));
        ribbon.enabled = true;
        ribbon.list = navList;
    };

    var isActivatedAsset = function (assetType) {
        var app = require('rxt').app;
        var activatedAssets = app.getActivatedAssets(ctx.tenantId); //ctx.tenantConfigs.assets;
        //return true;
        if (!activatedAssets) {
            throw 'Unable to load all activated assets for current tenant: ' + ctx.tenatId + '.Make sure that the assets property is present in the tenant config';
        }
        for (var index in activatedAssets) {
            if (activatedAssets[index] == assetType) {
                //log.info(activatedAssets[index] + "&" + assetType);
                return true;
            }
        }
        return false;
    };

    var buildAddLeftNav = function (page, util) {
        return [];
    };

    var buildListLeftNav = function (page, util) {
        var navList = util.navList();
        navList.push('ADD ' + type.toUpperCase(), 'fa-plus', util.buildUrl('create'));
        navList.push('All Statistics', 'fa-area-chart', '/asts/' + type + '/statistics');
        navList.push('Subscriptions', 'fa fa-bookmark', '/asts/' + type + '/statistics');
        navList.push('Statistics', 'fa-area-chart', '/asts/' + type + '/statistics');
        navList.push('Tier Permissions', 'fa fa-cog', '/asts/' + type + '/statistics');
        //navList.push('Configuration', 'icon-dashboard', util.buildUrl('configuration'));
        return navList.list();
    };

    var buildDefaultLeftNav = function (page, util) {
        var id = page.assets.id;
        var navList = util.navList();
        navList.push('Edit', 'fa-pencil', util.buildUrl('design') + '/' + id);
        navList.push('Overview', 'fa-list-alt', util.buildUrl('details') + '/' + id);
        navList.push('Life Cycle', 'fa-recycle', util.buildUrl('lifecycle') + '/' + id);
        navList.push('Versions', 'fa-recycle', util.buildUrl('versions') + '/' + id);
        navList.push('Docs', 'fa-recycle', util.buildUrl('docs') + '/' + id);
        navList.push('Users', 'fa-recycle', util.buildUrl('users') + '/' + id);
        return navList.list();
    };

    return {
        list: function(page) {
            var assets = page.assets;
            for (var index in assets) {
                var asset = assets[index];
                //Doing this because when there are no value specified in column such as thumbnail column it return string "null"
                // value which need be explicitly set to null
                if(asset.thumbnail == 'null') {
                    asset.thumbnail = null;
                }
            }
        },
        details: function (page) {
            //log.info(page);
            //Doing this because when there are no value specified in column such as thumbnail column it return string "null"
            // value which need be explicitly set to null
            if (page.assets.thumbnail == 'null') {
                page.assets.thumbnail = null;
            }
        },
        pageDecorators: {
            ribbon: function (page) {
                var ribbon = page.ribbon = {};
                ribbon.isSearchEnabled = true;
                var ribbon = page.ribbon = {};
                var DEFAULT_ICON = 'icon-cog';
                var assetTypes = [];
                var assetType;
                var assetList = ctx.rxtManager.listRxtTypeDetails();
                for (var index in assetList) {
                    assetType = assetList[index];
                    if (isActivatedAsset(assetType.shortName)) {
                        assetTypes.push({
                                            url:       this.buildBaseUrl(assetType.shortName) + '/list',
                                            assetIcon: assetType.ui.icon || DEFAULT_ICON,
                                            assetTitle: assetType.singularLabel
                                        });
                    }
                }
                ribbon.currentType = page.rxt.singularLabel;
                ribbon.currentTitle = page.rxt.singularLabel;
                ribbon.currentUrl = this.buildBaseUrl(type) + '/list'; //page.meta.currentPage;
                ribbon.shortName = page.rxt.singularLabel;
                ribbon.query = 'Query';
                ribbon.breadcrumb = assetTypes;
                return page;
            },
            leftNav: function (page) {
                if (log.isDebugEnabled()) {
                    log.debug('Using default leftNav');
                }
                switch (page.meta.pageName) {
                    case 'list':
                        page.leftNav = buildListLeftNav(page, this);
                        break;
                    case 'create':
                        page.leftNav = buildListLeftNav(page, this);
                        break;
                    case 'statistics':
                        page.leftNav = buildListLeftNav(page, this);
                        break;
                    default:
                        page.leftNav = buildDefaultLeftNav(page, this);
                        break;
                }
                return page;
            }
        }
    };
};
