(function () {
    document.querySelectorAll("table").forEach(function (table) {
        const existingWrapper = table.parentElement;
        if (existingWrapper.classList.contains("table-scroll-wrapper")) {
            return;
        }

        const wrapper = existingWrapper.classList.contains("table-container")
            ? existingWrapper
            : document.createElement("div");
        wrapper.classList.add("table-scroll-wrapper");
        if (!wrapper.hasAttribute("tabindex")) {
            wrapper.tabIndex = 0;
        }
        wrapper.setAttribute("role", "region");
        if (!wrapper.hasAttribute("aria-label")) {
            wrapper.setAttribute("aria-label", "Scrollable table");
        }

        if (wrapper !== existingWrapper) {
            table.parentNode.insertBefore(wrapper, table);
            wrapper.appendChild(table);
        }
    });
}());
