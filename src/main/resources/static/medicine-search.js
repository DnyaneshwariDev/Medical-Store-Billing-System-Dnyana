(function(){
    const PAGE_SIZE = 20;

    window.setupMedicinePrefixSearch = function(options){
        const input = document.getElementById(options.inputId);
        const dropdown = document.getElementById(options.dropdownId);
        const hiddenInput = document.getElementById(options.hiddenInputId);
        const nextFocus = document.getElementById(options.nextFocusId);
        const state = {
            abortController: null,
            debounceTimer: null,
            page: 0,
            totalPages: 0,
            query: "",
            requestId: 0
        };
        let activeIndex = -1;

        function closeDropdown(){
            dropdown.style.display = "none";
            activeIndex = -1;
        }

        function selectMedicine(option){
            hiddenInput.value = option.dataset.id;
            input.value = option.dataset.name;
            closeDropdown();
            nextFocus.focus();
        }

        function renderResults(medicines, query){
            dropdown.replaceChildren();
            const inputRect = input.getBoundingClientRect();
            dropdown.style.boxSizing = "border-box";
            dropdown.style.width = Math.min(
                Math.max(inputRect.width, 320),
                window.innerWidth - inputRect.left - 12
            ) + "px";

            if(medicines.length === 0){
                const message = document.createElement("div");
                message.textContent = `No medicines found for "${query}"`;
                dropdown.appendChild(message);
            }else{
                medicines.forEach(function(medicine){
                    const option = document.createElement("div");
                    option.dataset.id = medicine.id;
                    option.dataset.name = medicine.medicineName || "";
                    option.textContent = medicine.medicineName || "";
                    option.style.padding = "10px";
                    option.style.cursor = "pointer";
                    option.style.borderBottom = "1px solid #eee";
                    option.addEventListener("mouseenter", function(){
                        option.style.background = "#2196f3";
                        option.style.color = "white";
                    });
                    option.addEventListener("mouseleave", function(){
                        if(!option.classList.contains("active")){
                            option.style.background = "";
                            option.style.color = "";
                        }
                    });
                    dropdown.appendChild(option);
                });
            }

            if(state.totalPages > 1){
                const pagination = document.createElement("nav");
                pagination.className = "medicine-pagination";
                pagination.style.cssText =
                    "display:flex;align-items:center;justify-content:space-between;" +
                    "gap:8px;padding:6px 10px;border-top:1px solid #eee;" +
                    "background:white;color:#333;font-size:12px;";

                const previous = document.createElement("button");
                previous.type = "button";
                previous.dataset.page = state.page - 1;
                previous.textContent = "Prev";
                previous.disabled = state.page <= 0;

                const pageInfo = document.createElement("span");
                pageInfo.textContent =
                    `Page ${state.page + 1} of ${state.totalPages}`;

                const next = document.createElement("button");
                next.type = "button";
                next.dataset.page = state.page + 1;
                next.textContent = "Next";
                next.disabled = state.page + 1 >= state.totalPages;

                pagination.append(previous, pageInfo, next);
                dropdown.appendChild(pagination);
            }

            dropdown.style.display = "block";
        }

        async function fetchPage(query, page){
            if(state.abortController){
                state.abortController.abort();
            }

            const controller = new AbortController();
            const requestId = ++state.requestId;
            state.abortController = controller;

            try{
                const response = await fetch(
                    `/api/medicines/search?q=${encodeURIComponent(query)}` +
                    `&page=${page}&size=${PAGE_SIZE}`,
                    { signal: controller.signal }
                );
                if(!response.ok){
                    throw new Error("Medicine search failed");
                }

                const data = await response.json();
                if(requestId !== state.requestId ||
                   input.value.trim() !== query){
                    return;
                }

                state.page = Number.isInteger(data.number) ? data.number : page;
                state.totalPages = Number.isInteger(data.totalPages)
                    ? data.totalPages
                    : 0;
                state.query = query;
                renderResults(
                    Array.isArray(data.content) ? data.content : [],
                    query
                );
            }catch(error){
                if(error.name === "AbortError" ||
                   requestId !== state.requestId){
                    return;
                }

                console.error("Medicine search error:", error);
                dropdown.replaceChildren();
                const message = document.createElement("div");
                message.textContent =
                    "Medicine search failed. Please try again.";
                dropdown.appendChild(message);
                dropdown.style.display = "block";
            }
        }

        input.addEventListener("input", function(){
            const query = input.value.trim();
            hiddenInput.value = "";
            activeIndex = -1;

            if(state.debounceTimer){
                clearTimeout(state.debounceTimer);
            }
            if(state.abortController){
                state.abortController.abort();
            }
            state.requestId++;
            state.query = query;

            if(!query){
                dropdown.replaceChildren();
                closeDropdown();
                return;
            }

            dropdown.replaceChildren();
            const message = document.createElement("div");
            message.textContent = "Searching medicines...";
            dropdown.appendChild(message);
            dropdown.style.display = "block";
            state.debounceTimer = setTimeout(function(){
                fetchPage(query, 0);
            }, 250);
        });

        input.addEventListener("keydown", function(event){
            const items = dropdown.querySelectorAll("div[data-id]");

            if(event.key === "ArrowDown" && items.length){
                event.preventDefault();
                activeIndex = (activeIndex + 1) % items.length;
            }else if(event.key === "ArrowUp" && items.length){
                event.preventDefault();
                activeIndex = activeIndex <= 0
                    ? items.length - 1
                    : activeIndex - 1;
            }else if(event.key === "Enter"){
                event.preventDefault();
                event.stopPropagation();
                if(items.length){
                    selectMedicine(items[activeIndex < 0 ? 0 : activeIndex]);
                }else if(hiddenInput.value){
                    nextFocus.focus();
                }
                return;
            }else if(event.key === "Escape"){
                closeDropdown();
                return;
            }else{
                return;
            }

            items.forEach(item => {
                item.classList.remove("active");
                item.style.background = "";
                item.style.color = "";
            });
            if(activeIndex >= 0){
                items[activeIndex].classList.add("active");
                items[activeIndex].style.background = "#2196f3";
                items[activeIndex].style.color = "white";
                items[activeIndex].scrollIntoView({ block: "nearest" });
            }
        });

        dropdown.addEventListener("click", function(event){
            const pageButton = event.target.closest("button[data-page]");
            if(pageButton && !pageButton.disabled){
                event.preventDefault();
                fetchPage(state.query, Number(pageButton.dataset.page));
                return;
            }

            const option = event.target.closest("div[data-id]");
            if(option && dropdown.contains(option)){
                selectMedicine(option);
            }
        });

        document.addEventListener("click", function(event){
            if(!input.contains(event.target) &&
               !dropdown.contains(event.target)){
                closeDropdown();
            }
        });
    };
})();
