package com.maltino.net.talkback;

import android.location.Location;
import android.os.Environment;

import java.io.IOException;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.Locale;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;

public final class ObjectSerialization {

    private static ObjectSerialization Engine;
    private Gson serializer;
    private String fileRoot;

    private ObjectSerialization() {
        fileRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();

        serializer = new GsonBuilder()
                .registerTypeAdapter(Location.class, new LocationSerializer())
                .registerTypeAdapter(Location.class, new LocationDeserializer())
                .enableComplexMapKeySerialization()
                .serializeNulls()
                .setDateFormat(DateFormat.LONG)
                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                .setPrettyPrinting()
                .setVersion(1.0)
                .create();
    }

    public static ObjectSerialization getInstance() {
        if(Engine == null) {
            Engine = new ObjectSerialization();
        }
        return Engine;
    }

    public void SerializeToFile(Object obj, String filename) {

        String fullFile = getFullFilePathFor(filename);
        Writer w = null;
        try {
            w = new FileWriter(fullFile);
            serializer.toJson(obj, w);
            w.flush();
            System.out.println("Object serialized and saved to " + fullFile);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(w);
        }

    }

    public <T> T DeserializeFromFile(File jsonFile, Type targetType)  {

        T yoke = null;
        Reader f = null;
        try {
            f = new FileReader(jsonFile);
            yoke = serializer.fromJson(f,  targetType);
            System.out.println("Bounced back with " );
        } catch (JsonSyntaxException jsx) {
            viewFile(jsonFile);
            jsx.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            close(f);
        }

        return yoke;
    }

    private void viewFile(File filename) {
        InputStream fis = null;
        try {
            // open the file for reading
            fis = new FileInputStream(filename);

            // if file the available for reading
            if (fis != null) {

                // prepare the file for reading
                InputStreamReader chapterReader = new InputStreamReader(fis);
                BufferedReader buffreader = new BufferedReader(chapterReader);

                String line;

                // read every line of the file into the line-variable, on line at the time
                do {
                    line = buffreader.readLine();
                    // do something with the line
                    System.out.println(line);
                } while (line != null);

            }
        } catch (Exception e) {
            // print stack trace.
        } finally {
            // close the file.
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getFullFilePathFor(String filename) {
        return Paths.get(fileRoot, filename).toString();
    }

    private class LocationSerializer implements JsonSerializer<Location> {

        @Override
        public JsonElement serialize(Location src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(String.format(Locale.US, "%3.7f:%3.7f", src.getLatitude(), src.getLongitude()));
        }
    }

    private class LocationDeserializer implements JsonDeserializer<Location> {

        @Override
        public Location deserialize(JsonElement locationStr, Type typeOfSrc, JsonDeserializationContext context) {
            try {
                String[] parts = locationStr.getAsString().split(":");
                Location result = new Location("JSON");
                if (parts.length > 1) {
                    result.setLatitude(Double.parseDouble(parts[0]));
                    result.setLongitude(Double.parseDouble(parts[1]));
                }
                return result;
            }
            catch (JsonParseException e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    private void close(Closeable f) {
        if (f != null)
            try {
                f.close();
            } catch (IOException x) {
                x.printStackTrace();
            }
    }
}
