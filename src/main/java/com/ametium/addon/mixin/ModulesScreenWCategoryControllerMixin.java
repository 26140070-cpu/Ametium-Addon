package com.ametium.addon.mixin;

import com.ametium.addon.mixin.accessor.WidgetScreenAccessor;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.ModulesScreen;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Mixin(targets = "meteordevelopment.meteorclient.gui.screens.ModulesScreen$WCategoryController", remap = false)
public abstract class ModulesScreenWCategoryControllerMixin {

    @Shadow
    public List<WWindow> windows;

    @Shadow
    private Cell<WWindow> favorites;

    @Shadow
    private ModulesScreen this$0;

    @Unique
    private static final ScheduledExecutorService ametium$searchScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Ametium-Module-Search");
                t.setDaemon(true);
                return t;
            });

    @Unique
    private ScheduledFuture<?> ametium$pendingSearch;

    @Unique
    private String ametium$lastQuery = null;

    @Overwrite
    public void init() {
        ModulesScreen screen = this$0;
        GuiTheme theme = ((WidgetScreenAccessor) screen).getTheme();

        Map<String, Map<String, List<Module>>> grouped = new LinkedHashMap<>();
        for (Category category : Modules.loopCategories()) {
            for (Module module : Modules.get().getGroup(category)) {

                String fullName = category.name;
                String parent, child;
                int slash = fullName.indexOf('/');
                if (slash == -1) {
                    parent = fullName;
                    child = "";
                } else {
                    parent = fullName.substring(0, slash);
                    child = fullName.substring(slash + 1);
                }
                grouped
                        .computeIfAbsent(parent, k -> new LinkedHashMap<>())
                        .computeIfAbsent(child, k -> new ArrayList<>())
                        .add(module);
            }
        }

        WContainer self = (WContainer) (Object) this;

        for (Map.Entry<String, Map<String, List<Module>>> parentEntry : grouped.entrySet()) {
            String parent = parentEntry.getKey();
            Map<String, List<Module>> children = parentEntry.getValue();

            WWindow window = theme.window(parent);
            window.id = parent;

            self.add(window);
            windows.add(window);

            for (Map.Entry<String, List<Module>> childEntry : children.entrySet()) {
                String child = childEntry.getKey();
                List<Module> modules = childEntry.getValue();

                if (!child.isEmpty()) {
                    window.add(theme.horizontalSeparator(child)).expandX();
                }

                for (Module module : modules) {
                    window.add(theme.module(module)).expandX();
                }
            }
        }

        WWindow searchWindow = theme.window("Search");
        searchWindow.id = "search";
        if (theme.categoryIcons()) {
            searchWindow.beforeHeaderInit = wContainer -> wContainer.add(theme.item(new ItemStack(Items.COMPASS))).pad(2);
        }
        self.add(searchWindow);
        windows.add(searchWindow);

        if (searchWindow.view != null) {
            searchWindow.view.scrollOnlyWhenMouseOver = true;
            searchWindow.view.hasScrollBar = false;
            searchWindow.view.maxHeight -= 20;
        }

        WVerticalList searchList = theme.verticalList();

        WHorizontalList searchBar = theme.horizontalList();
        WTextBox searchTextBox = theme.textBox("");
        searchTextBox.setFocused(true);
        searchBar.add(searchTextBox).minWidth(120).expandX();

        WButton clearButton = theme.button("X");
        clearButton.action = () -> {
            searchTextBox.set("");
            ametium$runSearch(theme, searchList, "");
        };
        searchBar.add(clearButton);

        searchWindow.add(searchBar).expandX();
        searchWindow.add(searchList).expandX();

        searchTextBox.action = () -> {
            String text = searchTextBox.get().trim();

            if (ametium$pendingSearch != null && !ametium$pendingSearch.isDone()) {
                ametium$pendingSearch.cancel(false);
            }

            ametium$pendingSearch = ametium$searchScheduler.schedule(() ->
                            MinecraftClient.getInstance().execute(() -> ametium$runSearch(theme, searchList, text)),
                    300, TimeUnit.MILLISECONDS);
        };
    }

    @Unique
    private void ametium$runSearch(GuiTheme theme, WVerticalList searchList, String text) {
        if (text.equals(ametium$lastQuery)) return;
        ametium$lastQuery = text;

        searchList.clear();
        if (text.isEmpty()) return;

        boolean useAliases = Config.get().moduleAliases.get();
        int maxResults = Config.get().moduleSearchCount.get();

        List<Map.Entry<Module, Integer>> scored = new ArrayList<>();
        for (Module module : Modules.get().getAll()) {
            Integer score = ametium$fuzzyScore(module.name, text);

            if (useAliases) {
                for (String alias : module.aliases) {
                    Integer aliasScore = ametium$fuzzyScore(alias, text);
                    if (aliasScore != null && (score == null || aliasScore > score)) {
                        score = aliasScore - 5;
                    }
                }
            }

            if (score != null) scored.add(new AbstractMap.SimpleEntry<>(module, score));
        }
        scored.sort((a, b) -> b.getValue() - a.getValue());

        if (scored.size() > maxResults) scored = scored.subList(0, maxResults);

        if (scored.isEmpty()) {
            searchList.add(theme.label("No modules founded")).expandX();
            return;
        }

        searchList.add(theme.horizontalSeparator("Modules")).expandX();
        for (Map.Entry<Module, Integer> entry : scored) {
            searchList.add(theme.module(entry.getKey())).expandX();
        }
    }

    @Unique
    private static Integer ametium$fuzzyScore(String text, String pattern) {
        if (pattern.isEmpty()) return 0;

        String lowerText = text.toLowerCase();
        String lowerPattern = pattern.toLowerCase();

        int score = 0, ti = 0, consecutive = 0;
        for (int pi = 0; pi < lowerPattern.length(); pi++) {
            char pc = lowerPattern.charAt(pi);
            boolean found = false;

            while (ti < lowerText.length()) {
                if (lowerText.charAt(ti) == pc) {
                    score += 10;
                    if (consecutive > 0) score += consecutive * 5;
                    if (ti == 0 || !Character.isLetterOrDigit(lowerText.charAt(ti - 1))) score += 15;
                    consecutive++;
                    ti++;
                    found = true;
                    break;
                } else {
                    consecutive = 0;
                    ti++;
                }
            }
            if (!found) return null;
        }

        score -= (lowerText.length() - lowerPattern.length());
        return score;
    }
}