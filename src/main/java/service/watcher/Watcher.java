package service.watcher;

import com.google.gson.Gson;
import model.huesensordata.HueIndividualSensorData;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;
import service.api.Api;
import service.mqtt.MqttManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Watcher {
    Api api;
    MqttManager mqttManager;
    List<Integer> listOfSensorsIdToWatch;
    public Map<Integer, Boolean> alertMap;
    public List<Thread> watcherThreadPool;
    private boolean state = false;

    public Watcher() throws IOException, MqttException {
        listOfSensorsIdToWatch = new ArrayList<>();
        api = new Api();
        mqttManager = new MqttManager();

        alertMap = new HashMap<>();
        watcherThreadPool = new ArrayList<>();
    }

    public boolean start() {
        state = true;
        for(Integer id: listOfSensorsIdToWatch){
            watcherThreadPool.add(new Thread(() -> {
                try {
                    SensorWatcherThreadLoop(id);
                } catch (InterruptedException e) {
                    System.out.println("Sensor alert watcher for Id=" + id + " terminated");
                }
            }));
        }

        return state;
    }

    public boolean stop() {
        state = false;


        return state;
    }


    public void SensorWatcherThreadLoop(Integer id) throws InterruptedException {
        while(true){
            try {
                Map<Integer, Boolean> alertMap = checkSensorsState();

                System.out.println("Alertmap size:" + alertMap.size());
                if(alertMap.size() > 0){
                    JSONObject json = new JSONObject();

                    for(Map.Entry<Integer, Boolean> entry: alertMap.entrySet()){
                        json.put( entry.getKey().toString(), entry.getValue());
                    }
                    api.pushNotification(json.toString());
                }
                Thread.sleep((int) (2000));
            } catch (InterruptedException | IOException e) {
                System.out.println("Thread loop crashed: " + e.getMessage());
            }
        }
    }

    public Map<Integer, Boolean> checkSensorsState() throws IOException {
        try {
            //get sensors state
            String jsonString = api.getSensorsState();
            JSONObject jsonObject;
            JSONObject sensorJsonObject;

            Map<Integer, Boolean> alertMap = new HashMap<>();

            for (Integer id : listOfSensorsIdToWatch) {
                jsonObject = new JSONObject(jsonString);
                sensorJsonObject = jsonObject.getJSONObject(id.toString());

                System.out.println("Sensor data string retrieved for id=" + id.toString() + "\r\n" + sensorJsonObject.toString());

                Gson g = new Gson();
                HueIndividualSensorData sensorData = g.fromJson(sensorJsonObject.toString(), HueIndividualSensorData.class);

                if(sensorData.state.presence){
                    alertMap.put(id, sensorData.state.presence);
                }
            }
            return alertMap;
        } catch (
                Exception e) {
            System.out.println("Error while parsing sensor data:" + e.getMessage());
            return alertMap;
        }
    }

    public boolean checkSensorState(Integer id) throws IOException {
        try {
            //get sensors state
            String jsonString = api.getSensorsState();
            JSONObject jsonObject;
            JSONObject sensorJsonObject;
            String name;
            Boolean presence;
            String producName;

            jsonObject = new JSONObject(jsonString);
            sensorJsonObject = jsonObject.getJSONObject(id.toString());

            System.out.println("Sensor data string retrieved for id=" + id.toString() + "\r\n" + sensorJsonObject.toString());

            Gson g = new Gson();
            HueIndividualSensorData sensorData = g.fromJson(sensorJsonObject.toString(), HueIndividualSensorData.class);

            System.out.println("Sensor presence state=" + sensorData.state.presence);
            System.out.println("Sensor name=" + sensorData.name);
            System.out.println("Sensor productName=" + sensorData.productname);

            return true;
        } catch (
                Exception e) {
            System.out.println("Error while parsing sensor data:" + e.getMessage());
            return false;
        }
    }
}