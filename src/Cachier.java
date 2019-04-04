import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.function.Function;

public class Cachier
{
    public static int cache_expires = 900; //default 15 mins
    public static String resp_cache_dir = Util.AppRootDir + "cached_responses/";

    public static <T, R extends EwsApiWrapper.Response> R GetCached(T req, Function<T, R> callback) throws Exception
    {
        R resp;
        var cacheFile = resp_cache_dir + Util.getMD5(req);

        if (IsMissingOrExpired(cacheFile))
        {
            resp = callback.apply(req);
            if (resp != null && resp.ErrorMsg == null) {//do not save cache when there was an error
                Files.createDirectories(Paths.get(resp_cache_dir));
                KryoSave(cacheFile, resp);
            }
        }
        else
            resp = (R)KryoLoad(cacheFile);

        return resp;
    }

    /**
     * Check whether the cache file specified is missing or expired. The cache file will be deleted when found expired.
     * @param cacheFile The path to the cache file.
     * @param expireSec Cache lifespan in seconds.
     * @return False if the cache is still valid. True when the cache file is missing or expired.
     */
    public static boolean IsMissingOrExpired(String cacheFile, int expireSec) throws IOException
    {
        var path = Paths.get(cacheFile);
        if (!Files.exists(path)) return true;

        if (Instant.now().plusSeconds(-expireSec).isAfter(((FileTime)Files.getAttribute(path, "creationTime")).toInstant()))
        {
            Files.delete(path);//delete cache file when expired
            return true;
        }
        return false;
    }
    /**
     * Check whether the cache file specified is missing or expired with the default threshold (1 day).
     * The cache file will be deleted when found expired.
     */
    public static boolean IsMissingOrExpired(String cacheFile) throws IOException {
        return IsMissingOrExpired(cacheFile, cache_expires);
    }


    private static Object KryoLoad(Kryo kryo, File file) throws FileNotFoundException {
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        Object map;
        try (var strmIn = new Input(new FileInputStream(file))) {
            map = kryo.readClassAndObject(strmIn);
        }
        return map;
    }
    private static Object KryoLoad(File file) throws FileNotFoundException {
        return KryoLoad(new Kryo(), file);
    }
    private static Object KryoLoad(String file) throws FileNotFoundException {
        return KryoLoad(new Kryo(), new File(file));
    }


    private static void KryoSave(Kryo kryo, File file, Object obj) throws FileNotFoundException {
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        try (var strmOut = new Output(new FileOutputStream(file))) {
            kryo.writeClassAndObject(strmOut, obj);
        }
    }
    private static void KryoSave(File file, Object obj) throws FileNotFoundException {
        KryoSave(new Kryo(), file, obj);
    }
    private static void KryoSave(String file, Object obj) throws FileNotFoundException {
        KryoSave(new Kryo(), new File(file), obj);
    }
}
