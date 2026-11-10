package appeng.client.gui.me.filterterminal;

import java.util.Locale;

final class FilterTerminalSearch {

    private FilterTerminalSearch() {
    }

    static boolean matches(FilterTerminalRecord record, String nameSearch, String configuredSearch) {
        return matchesName(record, normalize(nameSearch))
                && matchesConfigured(record, normalize(configuredSearch));
    }

    private static boolean matchesName(FilterTerminalRecord record, String search) {
        return search.isEmpty() || record.getSearchName().contains(search);
    }

    private static boolean matchesConfigured(FilterTerminalRecord record, String search) {
        if (search.isEmpty()) {
            return true;
        }

        for (var slot = 0; slot < record.getInventory().size(); slot++) {
            var stack = record.getInventory().getStack(slot);
            if (stack != null && normalize(stack.what().getDisplayName().getString()).contains(search)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
