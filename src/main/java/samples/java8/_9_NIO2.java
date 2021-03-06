package samples.java8;

import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

//1 D - C
//2 D - B
//3 B +
//4 A +
//5 D - A

public class _9_NIO2 {

    private static final String ABSOLUTE_PATH_TO_GAMES = "C:\\Users\\vasylk\\Projects\\trunk\\ngm\\client\\games";
    private static final String ABSOLUTE_PATH_TO_BRANDS = "C:\\Users\\vasylk\\Projects\\ngm_brands";
    private static final String CONFIG_FILE_NAME = "config.json";
    private static final String BRAND_CONFIG_FILE_NAME = "brand.json";
    private static final String CONTENT_TO_INSERT = "  \"someNewParameterName\": \"someNewParameterValue\"";
    private static final String LAST_LINE_IN_SEARCH_FILE = "}";
    private static final String ADD_MENU_BUTTONS_PARAM_NAME = "addMenuButtons";
    private static final String MAIN_MENU_ITEMS_PARAM_NAME = "mainMenuItems";
    private static final String TABLE_GAME_PARAM_NAME = "tableGame";
    private static final String VISIBLE_IN_MODE_PARAM_NAME = "visibleInMode";
    public static final String ENABLED_IN_MODE_PARAM_NAME = "enabledInMode";
    public static final String PLATFORM_CONFIG_JSON_FILE_PATH = "C:\\Users\\vasylk\\Projects\\platform\\platform-html\\src\\main\\webapp\\json\\config.json";
    public static final String GAME_CONFIG_PARAM_NAME = "gameConfig";
    public static final int MIN_PERCENT_FOR_ENABLED_BY_DEFAULT = 51;

    public static void main(String[] args) throws IOException, NoSuchFieldException, IllegalAccessException {
//        Path path = Paths.get("C:\\Users\\vasylk\\Projects\\platform\\platform-html\\src\\main\\webapp\\json\\config.json");
//        JsonParameters jsonParameters = readMainMenuItems(path, getToVisibleInModeMapper());
//        processGames("C:\\Users\\vasylk\\Desktop\\gamesAnalysis.txt", false);
//        processGames(null, false);
        processBrands("C:\\Users\\vasylk\\Desktop\\brandsAnalysis.txt");
//        processBrands(null);
    }

    private static void processGames(String outputFileName, boolean isNeedToUpdate) throws IOException {
        PrintStream out = null;
        if (outputFileName != null) {
            out = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(outputFileName))));
            System.setOut(out);
        }
        Path pathToGames = Paths.get(ABSOLUTE_PATH_TO_GAMES);
        System.out.println("PROCESSING OF GAMES...");
        Predicate<Path> isNotHiddenDirectory = p -> !p.getFileName().toString().startsWith(".") && Files.isDirectory(p);
        String fileToSearch = CONFIG_FILE_NAME;
        Map<String, List<Path>> filesPaths = findFilesPaths(pathToGames, isNotHiddenDirectory, fileToSearch);
        printProcessedInfo(fileToSearch, filesPaths);

        JSONObject platformConfig = createJsonObjectFromFile(Paths.get(PLATFORM_CONFIG_JSON_FILE_PATH));
        String configParamToFetch = GAME_CONFIG_PARAM_NAME;
        if (platformConfig.has(configParamToFetch)) {
            platformConfig.getJSONObject(configParamToFetch).keySet().stream()
                    .map(getParamToPairMapper(filesPaths))
                    .sorted((p1, p2) -> new Integer(p2.getValue().size()).compareTo(p1.getValue().size()))
                    .forEach(getParamsInfoPrinter(fileToSearch));
        }

        System.out.println();
        System.out.println();
        if (platformConfig.has(MAIN_MENU_ITEMS_PARAM_NAME)) {
            getParamsInfoPrinter(fileToSearch).accept(getParamToPairMapper(filesPaths).apply(MAIN_MENU_ITEMS_PARAM_NAME));
        }

        if (isNeedToUpdate) {
            updateGamesConfigs(filesPaths);
        }

        if (outputFileName != null) {
            out.close();
        }
    }

    private static void processBrands(String outputFileName) throws IOException, NoSuchFieldException, IllegalAccessException {
        PrintStream out = null;
        if (outputFileName != null) {
            out = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(outputFileName))));
            System.setOut(out);
        }
        Path pathToBrands = Paths.get(ABSOLUTE_PATH_TO_BRANDS);
        System.out.println();
        System.out.println("PROCESSING OF BRANDS ...");

        Predicate<Path> isNotHiddenDirectoryFilter = p -> !p.getFileName().toString().startsWith(".") && Files.isDirectory(p);
        Map<String, List<Path>> filesPaths = findFilesPaths(pathToBrands, isNotHiddenDirectoryFilter, BRAND_CONFIG_FILE_NAME);

        printProcessedInfo(BRAND_CONFIG_FILE_NAME, filesPaths);
        List<String> processedRootsNames = filesPaths.keySet().stream().sorted().collect(Collectors.toList());
        if (!processedRootsNames.isEmpty()) {
//            System.out.println();
//            System.out.println("PROCESSED BRANDS (" + processedRootsNames.size() + "):");
//            processedRootsNames.forEach(System.out::println);
        }

        Path pathToPlatformsConfigFile = Paths.get(PLATFORM_CONFIG_JSON_FILE_PATH);
        JsonParameters defaultItems = readMainMenuItems(pathToPlatformsConfigFile, getToVisibleInModeMapper());

//        printBrandsConfigs(findBrandsThantHaveConfigs(filesPaths));
        printOverridingParametersInfo(pathToPlatformsConfigFile, findBrandsThantHaveConfigs(filesPaths));

//        System.out.println();
//        System.out.println();
//        System.out.print("BRANDS mainMenuItems OVERRIDDEN VALUES:");
//        System.out.println();
        List<BrandInfo> brandsInfo = collectBrandsInfo(filesPaths);
//        brandsInfo.forEach(System.out::println);
        System.out.println();


        printMainMenuItemsEnabledInModeDefaultValuesAnalysis(filesPaths, defaultItems, brandsInfo);
        printDefaultMainMenuItemsEnabledInModeValues(defaultItems);

        if (outputFileName != null) {
            out.close();
        }
    }

    private static JsonParameters readMainMenuItems(Path path, Function<Object, Pair<String, JSONObject>> mapper) throws IOException {
        Object parameter = getParameter(path, MAIN_MENU_ITEMS_PARAM_NAME);
        if (parameter != null && JSONArray.class.getName().equals(parameter.getClass().getName())) {
            return getParametersList((JSONArray) parameter, mapper, getMainMenuItemComparator());
        }
        return new JsonParameters(new ArrayList<>());
    }

    private static Object getParameter(Path pathToFile, String paramName) throws IOException {
        JSONObject jsonObject = createJsonObjectFromFile(pathToFile);
        if (jsonObject.has(paramName)) return jsonObject.get(paramName);
        else return null;
    }

    private static JSONObject createJsonObjectFromFile(Path pathToFile) throws IOException {
        String fileContent = lines(pathToFile).reduce("", (a, b) -> a + b);
        return new JSONObject(fileContent);
    }

    private static JsonParameters getParametersList(JSONArray array, Function<Object, Pair<String, JSONObject>> paramMapper, Comparator<Object> paramComparator) {
        Stream<Object> stream = stream(array.spliterator(), false);
        List<Pair<String, JSONObject>> list;
        if (paramComparator != null)
            list = stream.sorted(paramComparator).map(paramMapper).collect(Collectors.toList());
        else list = stream.map(paramMapper).collect(Collectors.toList());
        return new JsonParameters(list);
    }

    private static Function<Object, Pair<String, JSONObject>> getToEnabledInModeMapper() {
        return item -> {
            JSONObject button = (JSONObject) item;
            String buttonId = button.getString("id");
            JSONObject enabledInMode = new JSONObject("{}");
            if (button.has(ENABLED_IN_MODE_PARAM_NAME)) {
                return new Pair<>(buttonId, button.getJSONObject(ENABLED_IN_MODE_PARAM_NAME));
            } else return new Pair<>(buttonId, enabledInMode);
        };
    }

    private static Function<Object, Pair<String, JSONObject>> getToVisibleInModeMapper() {
        return item -> {
            JSONObject button = (JSONObject) item;
            String buttonId = button.getString("id");
            JSONObject enabledInMode = new JSONObject("{}");
            if (button.has(VISIBLE_IN_MODE_PARAM_NAME)) {
                return new Pair<>(buttonId, button.getJSONObject(VISIBLE_IN_MODE_PARAM_NAME));
            } else return new Pair<>(buttonId, enabledInMode);
        };
    }

    private static Comparator<Object> getMainMenuItemComparator() {
        return (o1, o2) -> {
            JSONObject button1 = (JSONObject) o1;
            JSONObject button2 = (JSONObject) o2;
            String buttonId1 = button1.getString("id");
            String buttonId2 = button2.getString("id");
            return buttonId1.compareTo(buttonId2);
        };
    }

    private static void normalized() {
        Path path = Paths.get(".");
        Path normalized = path.normalize();
        int nameCount = normalized.getNameCount();
        System.out.println(nameCount);
    }

    private static void symbolicLinks() throws IOException {
        Path path = Paths.get("../");
        Files.find(path, 0, (p, a) -> a.isSymbolicLink())
                .map(p -> p.toString())
                .collect(Collectors.toList())
                .stream()
                .filter(x -> x.endsWith(".txt"))
                .forEach(System.out::println);
    }

    private static void relativize() {
        Path path = Paths.get("/user/.././root", "../kodiacbear.txt");
        path = path.normalize().relativize(Paths.get("/lion"));
        System.out.println(path);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getJsonParameters(Path path) throws IOException, NoSuchFieldException, IllegalAccessException {
        JSONObject jsonObject = createJsonObjectFromFile(path);
        Field field = jsonObject.getClass().getDeclaredField("map");
        field.setAccessible(true);
        return (Map<String, Object>) field.get(jsonObject);
    }

    private static void printMainMenuItemsEnabledInModeDefaultValuesAnalysis(Map<String, List<Path>> filesPaths, JsonParameters defaultItems, List<BrandInfo> brandsInfo) {
        System.out.println();
        System.out.println();
        System.out.println();
        stream(defaultItems.spliterator(), false)
                .map(pairToItemPairInfoMapper(brandsInfo))
                .sorted((p1, p2) -> p2.getValue() - p1.getValue())
                .forEach(p -> {
                    String itemName = p.getKey();
                    int enabledCount = p.getValue();
                    double brandsCount = filesPaths.entrySet().size();
                    double part = Math.round(10000 * enabledCount / brandsCount) / 100.0;
                    System.out.println("ITEM WITH NAME " + itemName + " ENABLED IN " + enabledCount + " BRAND" + (enabledCount == 1 ? "" : "S") +
                            " - IT'S " + part + "%" + " => DEFAULT VALUE IS " + (part > MIN_PERCENT_FOR_ENABLED_BY_DEFAULT ? "TRUE" : "FALSE"));
                });
    }

    private static Function<Pair<String, JSONObject>, Pair<String, Integer>> pairToItemPairInfoMapper(List<BrandInfo> brandsInfo) {
        return itemPair -> {
            String mainMenuItemName = itemPair.getKey();
            JSONObject visibleInMode = itemPair.getValue();

            String offlineName = "offline";
            String onlineRealName = "online_Real";
            String onlineDemoName = "online_Demo";

            boolean offline = visibleInMode.getBoolean(offlineName);
            boolean onlineReal = visibleInMode.getBoolean(onlineRealName);
            boolean onlineDemo = visibleInMode.getBoolean(onlineDemoName);

            System.out.println();
            System.out.println("BRANDS THAT OVERRIDES " + itemPair.getKey() + " PARAMETER:");
            int itemEnabledCount = (int) brandsInfo.stream().filter(brandInfo -> {
                JSONObject enabledInMode = brandInfo.getMainMenuItems().get(mainMenuItemName);
                boolean resultOffline = offline;
                boolean resultOnlineReal = onlineReal;
                boolean resultOnlineDemo = onlineDemo;
                if (enabledInMode == null) return resultOffline && resultOnlineReal && resultOnlineDemo;
                if (enabledInMode.has(offlineName)) resultOffline = enabledInMode.getBoolean(offlineName);
                if (enabledInMode.has(onlineRealName))
                    resultOnlineReal = enabledInMode.getBoolean(onlineRealName);
                if (enabledInMode.has(onlineDemoName))
                    resultOnlineDemo = enabledInMode.getBoolean(onlineDemoName);
                boolean isEnabled = resultOffline || resultOnlineReal || resultOnlineDemo;
                if (isEnabled) System.out.println(brandInfo);
                return isEnabled;
            }).count();

            return new Pair<>(itemPair.getKey(), itemEnabledCount);
        };
    }


    private static List<BrandInfo> collectBrandsInfo(Map<String, List<Path>> filesPaths) {
        return findBrandsThantHaveConfigs(filesPaths).entrySet().stream()
                .map(_9_NIO2::createBrandInfo)
                .filter(b -> !b.brandJsons.isEmpty())
                .sorted((b1, b2) -> b1.brandName.compareTo(b2.brandName)).collect(Collectors.toList());
    }

    private static void printDefaultMainMenuItemsEnabledInModeValues(JsonParameters defaultItems) throws IOException {
        System.out.println();
        System.out.println("PLATFORM config.json FILE DEFAULT mainMenuItems[i].enabledInMode VALUES:");
        defaultItems.forEach(System.out::println);
    }

    private static Map<String, List<Pair<Path, JSONObject>>> findBrandsThantHaveConfigs(Map<String, List<Path>> filesPaths) {
        return filesPaths.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, getPathsToPairsMapper()));
    }

    private static void printBrandsConfigs(Map<String, List<Pair<Path, JSONObject>>> brandsThatHaveConfigs) {
        brandsThatHaveConfigs.entrySet()
                .stream()
                .filter(e -> !e.getValue().isEmpty())
                .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                .forEach(e -> {
                    System.out.println();
                    System.out.println("BRAND: " + e.getKey());
                    e.getValue().forEach(pair -> {
                        System.out.println(pair.getKey() + ":");
                        System.out.println();
                        System.out.println(pair.getValue());
                    });
                });
    }

    private static void printOverridingParametersInfo(Path pathToPlatformConfigFile, Map<String, List<Pair<Path, JSONObject>>> brandsThatHaveConfigs) throws IllegalAccessException, NoSuchFieldException, IOException {
        Map<String, Object> jsonParameters = getJsonParameters(pathToPlatformConfigFile);
        System.out.println();
        jsonParameters.keySet().stream()
                .collect(Collectors.toMap(e -> e, e -> findCount(brandsThatHaveConfigs).apply(e)))
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getValue() - e1.getValue())
                .forEach(e -> System.out.println("BRANDS THAT OVERRIDE " + e.getKey() + " PARAMETER (" + e.getValue() + ") :"));
    }

    private static Function<String, Integer> findCount(Map<String, List<Pair<Path, JSONObject>>> brandsThatHaveConfigs) {
        return param -> (int) brandsThatHaveConfigs.entrySet().stream().filter(brandsThatOverrideParameterFilter(param)).count();
    }

    private static Predicate<Map.Entry<String, List<Pair<Path, JSONObject>>>> brandsThatOverrideParameterFilter(String param) {
        return entry -> !entry.getValue().stream().filter(pair -> pair.getValue().has(param)).collect(Collectors.toList()).isEmpty();
    }

    private static Function<Map.Entry<String, List<Path>>, List<Pair<Path, JSONObject>>> getPathsToPairsMapper() {
        return e -> {
            List<Pair<Path, JSONObject>> pairs = e.getValue().stream().map(path -> {
                try {
                    return new Pair<>(path, new JSONObject(lines(path).reduce("", (a, b) -> a + b)));
                } catch (IOException cause) {
                    System.err.println("!!!!!!!!!ERRORR!!!!!!!!! - IO_EXCEPTION FOR: " + path.getFileName() + ". " + cause.getMessage());
                    return new Pair<>(path, new JSONObject("{}"));
                }
            }).collect(Collectors.toList());
            return pairs;
        };
    }

    private static BrandInfo createBrandInfo(Map.Entry<String, List<Pair<Path, JSONObject>>> e) {
        return new BrandInfo(e.getKey(), collectBrandJsonFilesForBrand(e));
    }

    private static List<BrandJson> collectBrandJsonFilesForBrand(Map.Entry<String, List<Pair<Path, JSONObject>>> e) {
        return e.getValue()
                .stream()
                .map(entry -> {
                    Path pathToFile = entry.getKey();
                    try {
                        return new BrandJson(pathToFile, readMainMenuItems(pathToFile, getToEnabledInModeMapper()));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        return null;
                    }
                }).collect(Collectors.toList());
    }

    private static Consumer<Pair<String, List<Map.Entry<String, List<Path>>>>> getParamsInfoPrinter(String fileToSearch) {
        return pair -> {
            List<Map.Entry<String, List<Path>>> haveParam = pair.getValue();
            System.out.println("GAMES THAT HAVE " + pair.getKey() + " PARAMETER IN ITS " + fileToSearch + " FILES (" + haveParam.size() + ") :");
//            pair.getValue().forEach(printInfo());
        };
    }

    private static Function<String, Pair<String, List<Map.Entry<String, List<Path>>>>> getParamToPairMapper(Map<String, List<Path>> filesPaths) {
        return paramName -> {
            List<Map.Entry<String, List<Path>>> haveParam = filesPaths.entrySet().stream()
                    .filter(e -> gameConfigsHasParameter(e, paramName))
                    .collect(Collectors.toList());
            return new Pair<>(paramName, haveParam);
        };
    }

    private static Consumer<Map.Entry<String, List<Path>>> printInfo() {
        return e -> System.out.println(e.getKey() + ": " +
                e.getValue().stream()
                        .map(p -> p.toString().split(e.getKey())[1] + "   ")
                        .reduce("", (a, b) -> a + b));
    }

    private static boolean gameConfigsHasParameter(Map.Entry<String, List<Path>> e, String paramName) {
        Stream<Path> stream = e.getValue().stream();
        List<Path> configsThatHasParam = stream.filter(gameConfigsHasParameter(paramName)).collect(Collectors.toList());
        return !configsThatHasParam.isEmpty();
    }

    private static Predicate<Path> gameConfigsHasParameter(String paramName) {
        return path -> {
            try {
                JSONObject jsonObject = createJsonObjectFromFile(path);
                return jsonObject.has(paramName);
            } catch (IOException cause) {
                System.err.println("!!!!!!!!!ERRORR!!!!!!!!! - (processGames) IO_EXCEPTION FOR: " + path.getFileName() + ". " + cause.getMessage());
                return false;
            }
        };
    }

    private static void updateGamesConfigs(Map<String, List<Path>> filesPaths) {
        System.out.println();
        AtomicInteger updated = new AtomicInteger(0);
        filesPaths.entrySet().stream()
                .filter(e -> e.getValue().size() == 1)
//                .limit(10)
                .forEach(e -> {
                    Path p = e.getValue().iterator().next();
                    try (Stream<String> linesStream = lines(p)) {
                        List<String> lines = linesStream.collect(toList());
                        if (!LAST_LINE_IN_SEARCH_FILE.equals(lines.get(lines.size() - 1).trim())) {
                            throw new IOException("WRONG FILE MARKING: LAST LINE IS NOT '" + LAST_LINE_IN_SEARCH_FILE + "'");
                        }
                        lines.set(lines.size() - 2, lines.get(lines.size() - 2) + ",");
                        lines.add(lines.size() - 1, CONTENT_TO_INSERT);
                        Files.write(p, lines, UTF_8);
                        System.out.println("FILE " + p + " IS SUCCESSFULLY UPDATED");
                        updated.getAndIncrement();
                    } catch (IOException cause) {
                        System.err.println("!!!!!!!!!ERRORR!!!!!!!!! - IO_EXCEPTION FOR: " + p.getFileName() + ". " + cause.getMessage());
                    }
                });
        System.out.println();
        System.out.println("UPDATED " + updated + " " + CONFIG_FILE_NAME + " FILES");
    }

    private static Map<String, List<Path>> findFilesPaths(Path pathToWorkDirectory, Predicate<Path> filterPredicate, String searchFileName) throws IOException {
        Map<String, List<Path>> searchFilePaths = new ConcurrentHashMap<>();
        Consumer<Path> processRootPath = p -> {
            String rootPathName = p.getFileName().toString();
            searchFilePaths.put(rootPathName, new ArrayList<>());
            try {
                Files.walk(p)
                        .filter(f -> !isDirectory(f) && searchFileName.equals(f.getFileName().toString()))
                        .forEach(f -> {
//                            System.out.println(f.getFileName() + " - " + f.toAbsolutePath());
                            searchFilePaths.get(rootPathName).add(f);
                        });
            } catch (IOException e) {
                System.err.println("IO_EXCEPTION FOR: " + p.getFileName());
            }
        };


        list(pathToWorkDirectory).filter(filterPredicate).forEach(processRootPath);
        return searchFilePaths;
    }

    private static void printProcessedInfo(String searchFileName, Map<String, List<Path>> searchFilePaths) {
        if (!searchFilePaths.entrySet().isEmpty()) {
            long foundFilesCount = searchFilePaths.values().stream().map(List::size).reduce(0, (a, b) -> a + b);
            System.out.println();
            System.out.println("FOUND " + foundFilesCount + " " + searchFileName + " FILES IN " + searchFilePaths.entrySet().size() + " ROOT DIRECTORIES");
        }

        List<String> emptyValues = searchFilePaths.entrySet().stream().filter(e -> e.getValue().isEmpty()).map(Map.Entry::getKey).collect(Collectors.toList());
        if (!emptyValues.isEmpty()) {
            System.out.println();
            System.out.println("ROOT DIRECTORIES WITHOUT " + searchFileName + " FILE (" + emptyValues.size() + "):");
            emptyValues.stream().sorted().forEach(System.out::println);
        }

        List<String> multipleValues = searchFilePaths.entrySet().stream().filter(e -> e.getValue().size() > 1).map(e -> e.getKey()).collect(Collectors.toList());
        if (!multipleValues.isEmpty()) {
            System.out.println();
            System.out.print("ROOT DIRECTORIES WITH MULTIPLE " + searchFileName + " FILES (" + multipleValues.size() + "):");
            searchFilePaths.entrySet().stream().filter(e -> e.getValue().size() > 1).sorted((p1, po2) -> p1.getKey().compareTo(po2.getKey())).forEach(e -> {
                System.out.println();
                System.out.print("" + e.getKey() + " - ");
                e.getValue().stream().forEach(p -> System.out.print(p.toString().split(e.getKey())[1] + "   "));
            });
            System.out.println();
            System.out.println();
        }
    }

    private static void filesAttributes() throws IOException {
        Files.readAttributes(Paths.get("README.md"), "dos:*", LinkOption.NOFOLLOW_LINKS).entrySet().forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
    }

    private static void basicFileAttributes() throws IOException {
        Path path = Paths.get("README.md");
        BasicFileAttributes basicFileAttributes = Files.readAttributes(
                path,
                BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS
        );
        Method[] declaredMethods = basicFileAttributes.getClass().getDeclaredMethods();

        Arrays.stream(declaredMethods)
                .filter(m -> {
                    int modifiers = m.getModifiers();
                    return isPublic(modifiers) && !isStatic(modifiers) && m.getParameterCount() == 0;
                })
                .forEach(m -> {
                    try {
                        m.setAccessible(true);
                        System.out.println(m.getName() + ": " + m.invoke(basicFileAttributes));
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        System.err.println(m.getName());
                    }
                });
    }

    private static void comparingPaths() {
        Path path1 = Paths.get("README.md");
        Path path2 = Paths.get("C:\\Users\\vasylk\\Projects\\Practice\\README.md");
        System.out.println(path1.getFileName());
        System.out.println(path1.getRoot());
        System.out.println(path1.toAbsolutePath());
        System.out.println(path2);

        System.out.println(path1 == path2);
        System.out.println(path1.equals(path2));
        System.out.println(path1.toAbsolutePath().equals(path2));
        System.out.println(path1.compareTo(path2));
        System.out.println(path1.toAbsolutePath().compareTo(path2));
    }
}


class BrandInfo {
    String brandName;
    List<BrandJson> brandJsons;

    public BrandInfo(String brandName, List<BrandJson> brandJsons) {
        this.brandName = brandName;
        this.brandJsons = brandJsons;
    }

    public JsonParameters getMainMenuItems() {
        return brandJsons.iterator().next().elements.getValue();
    }

    @Override
    public String toString() {
        return "\n" + brandName + ":\n" + brandJsons;
    }
}

class JsonParameters implements Iterable<Pair<String, JSONObject>> {
    List<Pair<String, JSONObject>> list;
    Map<String, JSONObject> map;

    public JsonParameters(List<Pair<String, JSONObject>> list) {
        this.list = list;
        map = new HashMap<>();
        for (Pair<String, JSONObject> pair : list) {
            map.put(pair.getKey(), pair.getValue());
        }
    }

    public JSONObject get(String string) {
        return map.get(string);
    }

    @Override
    public String toString() {
        return list.stream()
                .sorted((i1, i2) -> i1.getKey().compareTo(i2.getKey()))
                .map(p -> "\n" + p.getKey() + ": " + p.getValue())
                .reduce("", (a, b) -> a + b);
    }

    @Override
    public Iterator<Pair<String, JSONObject>> iterator() {
        return list.iterator();
    }

    @Override
    public void forEach(Consumer<? super Pair<String, JSONObject>> action) {
        list.forEach(action);
    }

    @Override
    public Spliterator<Pair<String, JSONObject>> spliterator() {
        return list.spliterator();
    }
}

class BrandJson {
    Pair<Path, JsonParameters> elements;

    public BrandJson(Path pathFile, JsonParameters buttons) {
        elements = new Pair<>(pathFile, buttons);
    }

    @Override
    public String toString() {
        return "\n" + elements.getKey() + ":" + elements.getValue();
    }
}


//Chapter 9 Test
//1 - D - F не знал, завтык,
//2 - C,D - B,C не знал
//3 - A - D не знал
//4 - C +
//5 - B,D - B,C,D не знал
//6 - C +
//7 - F +
//8 - E - A не знал
//9 - A,C - B,C не знал
//10 - B,E - C,E не знал
//11 - B - A не знал
//12 - A,F,!?G?! - A,F не знал
//13 - A - B не знал
//14 - A - E не знал
//15 - B,C - D,E,F не знал
//16 - A,E,F - F не знал
//17 - A,?E,F?,G - A,G не знал
//18 - D +
//19 - A,B,C,D,E,F - A,C,E не знал
//20 - D - B не знал
//-16(80%)
//Path.relativize(Path path) constructs a relative path between this path and a given path.
//Path::normalize does not change the current Path object, but instead it returns a new Path object
//Files.isDirectory(Path path) returns false if directory does not exist
//symbolic link can point to a real directory
//Files::deleteIfExists throws DirectoryNotEmptyException if directory has content
//BasicFileAttributes does not have the method setTimes()
//Path::subpath is applied to the absolute path, Path.getName() is applied to the relative path
//symbolic link can be replaced to path of referenced directory
//Files.find(Path,int,BiPredicate<Path,BasicFilesAttributes>,FileVisitOption...)
//Files.isSameFile(Path,Path) checks are they same in term of equals() if false, then checks existence of both path in the file system, relative and absolute to the same file is not equal
//Path::resolve does not normalize any path symbols, returns an absolute path if an argument is an absolute path
//path name can be with dot
//advantage of using Files.lines(): can be run on large files with very little memory available;
//if REPLACE_EXISTING flag is not provided then Files.move() throws an Exception if the target exists
//NOFOLLOW_LINKS flag means that if the source is a symbolic link, the link itself and not the target will be copied at runtime
//ATOMIC_MODE flag means that any process monitoring the file system will not see an incomplete file during the move
//possible to rename file from/to without extension
//Files.move(Path src, Path dst) if src is file/directory then dst will be file/directory as well
//Files.copy(Path src, Path dst, CopyOption...) src will not equal to dst, hence equals() return false
//FileSystems.getDefault().getPath(String path) exists
//Paths.getPath(String path) DOES not exist
//directory is regular and NOT a symbolic link
//Files.find(... int depth ...) if depth is 0 then search will be in top-level directory
//Files::list is most similar to File::listFiles and retrieves the members of the current directory without traversing any subdirectories
//Path::listFiles and Files::files DO NOT exist
//Files::walk and Files::find find recursively traverse a directory tree rather than list contents of the current directory
//Files.(...int depth...) can be similar to File::listFiles if depth is 1
//NIO.2 views advantages to read metadata rather than individually from Files methods: 1) makes fewer round-trip to the file system, 2) can be used to access file system-dependent attributes, 3) often more performant to read multiple attributes
//NIO.2 advantages over legacy File: 1) supports file system-dependent attributes; 2) supports symbolic links; 3) allows to traverse a directory tree directly
//NIO.2 and legacy File: 1) can be used to list all the files within a single directory; 2) can be used to delete files and non-empty directories; 3) can be used to read the last-modified time
//Path::normalize does not covert a relative path into an absolute path
//Path::getNameCount return 1 for current directory ("" or ".".normalize())
//Path has iterator. The first element returned by the iterator represents the name element that is closest to the root in the directory hierarchy, the second element is the next closest, and so on. The last element returned is the name of the file or directory denoted by this path


//    Predicate<Path> isHiddenCheck1 = p -> {
//        try {
//            return isHidden(p);
//        } catch (IOException e) {
//            System.err.println("IO_EXCEPTION for: " + p.getFileName());
//            return false;
//        }
//    };
//
//
//    Predicate<? super Path> isHiddenCheck2 = p -> {
//        Object attribute = null;
//        try {
//            attribute = Files.getAttribute(p, "dos:hidden", LinkOption.NOFOLLOW_LINKS);
//        } catch (IOException e) {
//            System.err.println("IO_EXCEPTION for: " + p.getFileName());
//        }
//        return attribute instanceof Boolean && (Boolean) attribute;
//
//    };

//                        int placeToInsert = content.lastIndexOf('}');
//                        String contentToInsert = ",  \"someNewParameterName\": \"someNewParameterValue\"";
//                        String newContent = content.substring(0, placeToInsert) + contentToInsert + content.substring(placeToInsert);