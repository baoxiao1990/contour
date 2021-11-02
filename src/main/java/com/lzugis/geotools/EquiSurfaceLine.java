package com.lzugis.geotools;

import com.amazonaws.util.json.JSONObject;
//import com.lzugis.CommonMethod;
//import com.lzugis.geotools.utils.FeaureUtil;
//
//import org.geotools.geojson.GeoJSONUtil;
import com.vividsolutions.jts.geom.Geometry;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import wcontour.Contour;
import wcontour.global.Border;
import wcontour.global.PointD;
import wcontour.global.PolyLine;
import wcontour.global.Polygon;
import wcontour.Interpolate;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.*;


/**
 * Created by admin on 2017/8/29.
 */
public class EquiSurfaceLine {
    private static String rootPath = System.getProperty("user.dir");

    /**
     * 生成等值面
     *
     * @param trainData    训练数据
     * @param dataInterval 数据间隔
     * @param size         大小，宽，高
     * @param boundryFile  四至
     * @param isclip       是否裁剪
     * @return
     */
    public String calEquiSurface(double[][] trainData,
                                 double[] dataInterval,
                                 int[] size,
                                 String boundryFile,
                                 boolean isclip) {
        String geojsonline = "";
        String geojsonpogylon = "";

        try {
            double _undefData = -9999.0;
            SimpleFeatureCollection polylineCollection = null;
            List<PolyLine> cPolylineList = new ArrayList<PolyLine>();
            List<Polygon> cPolygonList = new ArrayList<Polygon>();

            int width = size[0],
                    height = size[1];
            double[] _X = new double[width];
            double[] _Y = new double[height];

            File file = new File(boundryFile);
            ShapefileDataStore shpDataStore = null;

            shpDataStore = new ShapefileDataStore(file.toURL());
            //设置编码
            Charset charset = Charset.forName("GBK");
            shpDataStore.setCharset(charset);
            String typeName = shpDataStore.getTypeNames()[0];
            System.out.printf("typeName:%s\n", typeName);

            SimpleFeatureSource featureSource = null;
            featureSource = shpDataStore.getFeatureSource(typeName);
            SimpleFeatureCollection fc = featureSource.getFeatures();
            System.out.printf("FC:%s\n", fc.toArray());

            double minX = fc.getBounds().getMinX();
            double minY = fc.getBounds().getMinY();
            double maxX = fc.getBounds().getMaxX();
            double maxY = fc.getBounds().getMaxY();
            System.out.printf("%f,%f, %f, %f\n",minX, minY, maxX,maxY);

            Interpolate.createGridXY_Num(minX, minY, maxX, maxY, _X, _Y);
            double[][] _gridData = new double[width][height];

            int nc = dataInterval.length;

            Arrays.sort(trainData[0]);
            Arrays.sort(trainData[1]);



            _gridData = Interpolate.interpolation_IDW_Neighbor(trainData,
                    _X, _Y, 8, _undefData);// IDW插值



            System.out.printf("_X: %s\n",Arrays.toString(_X));
            System.out.printf("_Y: %s\n",Arrays.toString(_Y));

            System.out.printf("_gridData.length: %s\n",_gridData.length);
            System.out.printf("_gridData[0]:.length %s\n",_gridData[0].length);

            System.out.printf("_gridData0: %s\n",Arrays.toString(_gridData[0]));
            System.out.printf("_gridData1: %s\n",Arrays.toString(_gridData[1]));

            double gridMin = 0.0, gridMax = 0.0;
            for (int i = 0; i < _gridData.length; i++){
                for(int j = 0; j < _gridData[i].length; j++){
                    if(gridMax < _gridData[i][j]){
                        gridMax = _gridData[i][j];
                    }
                    if(gridMin > _gridData[i][j]){
                        gridMin = _gridData[i][j];
                    }
                }
            }

            System.out.printf("gridMin: %s, gridMax: %s\n",gridMin, gridMax);


            System.out.printf("dataInterval: %s\n",Arrays.toString(dataInterval));


            int[][] S1 = new int[_gridData.length][_gridData[0].length];
            /**
             * double[][] S0,
             * double[] X,
             * double[] Y,
             * int[][] S1,
             * double undefData
             */
            List<Border> _borders = Contour.tracingBorders(_gridData, _X, _Y,
                    S1, _undefData);

            /**
             * double[][] S0,
             * double[] X,
             * double[] Y,
             * int nc,
             * double[] contour,
             * double undefData,
             * List<Border> borders,
             * int[][] S1
             */
            cPolylineList = Contour.tracingContourLines(_gridData, _X, _Y, nc,
                    dataInterval, _undefData, _borders, S1);// 生成等值线

            cPolylineList = Contour.smoothLines(cPolylineList);// 平滑

            cPolygonList = Contour.tracingPolygons(_gridData, cPolylineList,
                    _borders, dataInterval);

            geojsonline = getPolylineGeoJson(cPolylineList);

            geojsonpogylon = getPolygonGeoJson(cPolygonList);


        } catch (Exception e) {
            e.printStackTrace();
        }
        return geojsonpogylon;
    }

    //拼装geogson
    public static String getPolygonGeoJson(List<Polygon> cPolygonList) {
        String geo = null;
        String geometry = " { \"type\":\"Feature\",\"geometry\":";
        String properties = ",\"properties\":{ \"value\":";

        String head = "{\"type\": \"FeatureCollection\"," + "\"features\": [";
        String end = "  ] }";
        if (cPolygonList == null || cPolygonList.size() == 0) {
            return null;
        }
        try {
            for (Polygon pPolygon : cPolygonList) {
                List<Object> ptsTotal = new ArrayList<Object>();
                for (PointD ptd : pPolygon.OutLine.PointList) {
                    List<Double> pt = new ArrayList<Double>();
                    pt.add(doubleFormat(ptd.X));
                    pt.add(doubleFormat(ptd.Y));
                    ptsTotal.add(pt);
                }
                List<Object> list3D = new ArrayList<Object>();
                list3D.add(ptsTotal);
                JSONObject js = new JSONObject();
                js.put("type", "Polygon");
                js.put("coordinates", list3D);

                geo = geometry + js.toString() + properties  +pPolygon.LowValue + "} }" + "," + geo;
            }
            if (geo.contains(",")) {
                geo = geo.substring(0, geo.lastIndexOf(","));
            }

            geo = head + geo + end;
        } catch (Exception e) {
            e.printStackTrace();
            return geo;
        }
        return geo;
    }

    /**
     * double保留两位小数
     */
    public static double doubleFormat(double d) {
        BigDecimal bg = new BigDecimal(d);
        double f1 = bg.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
        return f1;
    }


    public String getPolylineGeoJson(FeatureCollection fc) {
        FeatureJSON fjson = new FeatureJSON();
        StringBuffer sb = new StringBuffer();
        try {
            sb.append("{\"type\": \"FeatureCollection\",\"features\": ");
            FeatureIterator itertor = fc.features();
            List<String> list = new ArrayList<String>();
            while (itertor.hasNext()) {
                SimpleFeature feature = (SimpleFeature) itertor.next();
                StringWriter writer = new StringWriter();
                fjson.writeFeature(feature, writer);
                list.add(writer.toString());
            }
            itertor.close();
            sb.append(list.toString());
            sb.append("}");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }


    public String getPolylineGeoJson(List<PolyLine> cPolylineList) {
        String geo = null;
        String geometry = " { \"type\":\"Feature\",\"geometry\":";
        String properties = ",\"properties\":{ \"value\":";

        String head = "{\"type\": \"FeatureCollection\"," + "\"features\": [";
        String end = "  ] }";
        if (cPolylineList == null || cPolylineList.size() == 0) {
            return null;
        }
        try {
            for (PolyLine pPolyline : cPolylineList) {
                List<Object> ptsTotal = new ArrayList<Object>();

                for (PointD ptD : pPolyline.PointList) {
                    List<Double> pt = new ArrayList<Double>();
                    pt.add(ptD.X);
                    pt.add(ptD.Y);
                    ptsTotal.add(pt);
                }

                JSONObject js = new JSONObject();
                js.put("type", "LineString");
                js.put("coordinates", ptsTotal);

                geo = geometry + js.toString() + properties + pPolyline.Value + "} }" + "," + geo;
            }
            if (geo.contains(",")) {
                geo = geo.substring(0, geo.lastIndexOf(","));
            }

            geo = head + geo + end;
        } catch (Exception e) {
            e.printStackTrace();
            return geo;
        }
        return geo;
    }

    public static void append2File(String JsonPath, String messsage) {
        try {
            File f = new File(JsonPath);//向指定文本框内写入
            FileWriter fw = new FileWriter(f);
            fw.write(new String(messsage.getBytes(),"GBK"));
            fw.close();
        } catch (Exception e) {}
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        EquiSurfaceLine equiSurface = new EquiSurfaceLine();

        double[] bounds = {121.3110351600000030,30.5054839000000015,121.5307617200000010,30.6946115500000012};

        double[][] trainData = new double[100][3];

        for (int i = 0; i < 100; i++) {
            double x = bounds[0] + new Random().nextDouble() * (bounds[2] - bounds[0]),
                    y = bounds[1] + new Random().nextDouble() * (bounds[3] - bounds[1]),
                    v = 0 + new Random().nextDouble() * (45 - 0);
            trainData[i][0] = x;
            trainData[i][1] = y;
            trainData[i][2] = v;
        }

        double[] dataInterval = new double[20];

        for(int i = 0; i < dataInterval.length; i++){
            dataInterval[i] = i*45/dataInterval.length;
        }
        System.out.println(dataInterval);

        String boundryFile = rootPath + "/data/shp/XY_01_XZQH_PY.shp";

        int[] size = new int[]{100, 100};

        boolean isclip = true;

        try {
            String strJson = equiSurface.calEquiSurface(trainData, dataInterval, size, boundryFile, isclip);
            String strFile = rootPath + "/out/result.geojson";
            PrintStream ps = new PrintStream(strFile);
            System.setOut(ps);//把创建的打印输出流赋给系统。即系统下次向 ps输出

            System.out.printf("strJson:%s",strJson);
            equiSurface.append2File(strFile, strJson);

            String shpFile = rootPath + "/out/contour.shp";
            Map map = FileFormat.geojson2Shape(strFile, shpFile);
            System.out.println(map);

            //GeoServer的连接配置
            String url = "http://localhost:8080/geoserver" ;
            String username = "admin" ;
            String passwd = "geoserver" ;

            GeoServerUtil.GeoserverPublishShapefileData(url, username, passwd);
//            System.out.println(strFile + "插值成功, 共耗时" + (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}