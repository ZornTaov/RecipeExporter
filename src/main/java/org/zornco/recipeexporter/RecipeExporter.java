package org.zornco.recipeexporter;

import gregtech.api.GTValues;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMap;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.plugins.jei.JEIInternalPlugin;
import mezz.jei.recipes.RecipeRegistry;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

@Mod(modid = RecipeExporter.MODID, name = RecipeExporter.NAME, version = RecipeExporter.VERSION)
public class RecipeExporter {
    public static final String MODID = "recipeexporter";
    public static final String NAME = "Recipe Exporter";
    public static final String VERSION = "1.0";

    private static Logger logger;

    private static String FluidStackToString(FluidStack fluid) {
        return fluid.amount + "x" + fluid.getLocalizedName();
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        if (JEIInternalPlugin.jeiRuntime != null) {
            RecipeRegistry recipeRegistry = (RecipeRegistry) JEIInternalPlugin.jeiRuntime.getRecipeRegistry();
            logger.info("Registry found, size:" + recipeRegistry.getRecipeCategories().size());
            FileWriter file = null;
            try {
                file = new FileWriter("./RecipeOutput.json");
                JsonObject root = new JsonObject();


                JsonArray categories = new JsonArray();
                // get GregTech recipe stuff
                for (RecipeMap<?> recipeMap: RecipeMap.getRecipeMaps()) {
                    JsonObject cat = new JsonObject();
                    cat.put("Name", recipeMap.getLocalizedName());

                    IRecipeCategory<?> namespace = recipeRegistry.getRecipeCategory(GTValues.MODID + ":" + recipeMap.getUnlocalizedName());
                    if (namespace != null) {
                        List<Object> catalysts = recipeRegistry.getRecipeCatalysts(namespace,true);
                        JsonArray catalystsArray = itemStacksToJson(catalysts);
                        cat.put("Catalysts", catalystsArray);
                    }

                    JsonArray recipes = new JsonArray();
                    for (Recipe recipe: recipeMap.getRecipeList()) {
                        JsonObject rec = new JsonObject();
                        rec.put("inputs", GetItems(recipe.getInputs()));
                        rec.put("outputs", recipe.getOutputs().stream().map(this::getItemStackAsJson).collect(Collectors.toList()));
                        rec.put("chanceOutputs", itemStacksToJson(recipe.getChancedOutputs().keySet()));
                        rec.put("fluidInputs", recipe.getFluidInputs().stream().map(RecipeExporter::FluidStackToString).collect(Collectors.toList()));
                        rec.put("fluidOutputs", recipe.getFluidOutputs().stream().map(RecipeExporter::FluidStackToString).collect(Collectors.toList()));
                        rec.put("powerUsage", recipe.getEUt());
                        rec.put("duration", recipe.getDuration());
                        recipes.add( rec);
                    }
                    cat.put("recipes",recipes);

                    categories.add(cat);
                }
                root.put("categories", categories);
                root.toJson(file);
                /*Gson gson = new Gson();
                List<String> collect = recipeRegistry.getRecipeCategories().stream()
                        .flatMap(cat -> recipeRegistry.getCraftingItems(cat).stream())
                        .map(itemStack -> {return itemStack.toString();})
                        .collect(Collectors.toList());

                gson.toJson(collect, file);*/
                logger.info("Created RecipeOutput.json");

                //gson.toJson(recipeRegistry.getRecipeCategories().stream().map(Object::toString).toArray(), file);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (StackOverflowError e) {
                logger.error("whoops, overflowed");
            } finally {

                try {
                    if (file != null) {
                        file.flush();
                        file.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private <T> JsonArray itemStacksToJson(Collection<T> catalysts) {
        JsonArray catalystsArray = new JsonArray();
        for (Object catalyst : catalysts) {
            if (catalyst instanceof ItemStack)
                catalystsArray.add(getItemStackAsJson((ItemStack)catalyst));
            else
                catalystsArray.add(catalyst.toString());
        }
        return catalystsArray;
    }

    private JsonArray GetItems(List<CountableIngredient> ingredients) {
        JsonArray inputsArray = new JsonArray();
        for (CountableIngredient items : ingredients) {
            JsonArray itemsArray = new JsonArray();
            for (ItemStack item : items.getIngredient().getMatchingStacks()) {
                JsonObject itemObj = getItemStackAsJson(item);
                itemsArray.add(itemObj);
            }
            inputsArray.add(itemsArray);
        }
        return inputsArray;
    }

    private JsonObject getItemStackAsJson(ItemStack item) {
        JsonObject itemObj = new JsonObject();
        itemObj.put("count", item.getCount());
        itemObj.put("unlocalizedName", item.getItem().getTranslationKey());
        itemObj.put("name", item.getDisplayName());
        itemObj.put("damage", item.getItemDamage());
        itemObj.put("nbt", item.serializeNBT().toString());
        return itemObj;
    }
}
