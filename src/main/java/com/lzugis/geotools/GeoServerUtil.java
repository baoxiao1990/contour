package com.lzugis.geotools;


import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.decoder.RESTDataStore;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import it.geosolutions.geoserver.rest.encoder.datastore.GSShapefileDatastoreEncoder;
import org.apache.commons.httpclient.NameValuePair;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.List;

public class GeoServerUtil {
    public static void main(String[] args) throws IOException {
        //GeoServer的连接配置
        String url = "http://192.168.31.112:8080/geoserver" ;
        String username = "admin" ;
        String passwd = "geoserver" ;
        //GeoserverPublishPostGISData(url, username, passwd);
        GeoserverPublishShapefileData(url, username, passwd);
    }

    //发布shapefile数据
    public static void GeoserverPublishShapefileData(String url,String username,String passwd) throws IOException {

        String ws = "testshape" ;     //待创建和发布图层的工作区名称workspace
        String store_name = "testShapeStore" ; //待创建和发布图层的数据存储名称store
        String srs="EPSG:4326";
        //压缩文件的完整路径
        File zipFile=new File("D:/code/projects/javaProjs/geotoolsStartup2/out/contour.zip");
        String layername="contour";//图层名称
        //shp文件所在的位置
        String urlDatastore="file:/D:/code/projects/javaProjs/geotoolsStartup2/out/contour.shp";
        //判断工作区（workspace）是否存在，不存在则创建
        URL u = new URL(url);
        System.out.printf("u:%s\n", u);

        //获取管理对象
        GeoServerRESTManager manager = new GeoServerRESTManager(u, username, passwd);
        System.out.printf("manager:%s\n", manager);
        //获取发布对象
        GeoServerRESTPublisher publisher = manager.getPublisher() ;
        //获取所有的工作空间名称
        List<String> workspaces = manager.getReader().getWorkspaceNames();
        //判断工作空间是否存在
        if(!workspaces.contains(ws)){
            //创建一个新的存储空间
            boolean createws = publisher.createWorkspace(ws);
            System.out.println("create ws : " + createws);
        }else {
            System.out.println("workspace已经存在了,ws :" + ws);
        }

        //判断数据存储（datastore）是否已经存在，不存在则创建
        URL urlShapefile = new URL(urlDatastore);
        RESTDataStore restStore = manager.getReader().getDatastore(ws, store_name);
        if(restStore == null){
            //创建shape文件存储
            GSShapefileDatastoreEncoder store = new GSShapefileDatastoreEncoder(store_name, urlShapefile);
            boolean createStore = manager.getStoreManager().create(ws, store);
            System.out.println("create store : " + createStore);
        } else {
            System.out.println("数据存储已经存在了,store:" + store_name);
        }

        //判断图层是否已经存在，不存在则创建并发布
        RESTLayer layer = manager.getReader().getLayer(ws, layername);
        if(layer == null){
            //发布图层
            boolean publish = manager.getPublisher().publishShp(ws, store_name, layername, zipFile, srs);
            System.out.println("publish : " + publish);
        }else {
            System.out.println("表已经发布过了,table:" + store_name);
        }
    }

}
