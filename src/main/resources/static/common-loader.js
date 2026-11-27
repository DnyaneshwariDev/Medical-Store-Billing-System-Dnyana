window.CommonLoader = {
    activeSections: new Map(),

    runSection(sectionIds, load, label = "Loading..."){
        const ids = Array.isArray(sectionIds) ? sectionIds : [sectionIds];
        const key = ids.join("|");
        const activeRun = this.activeSections.get(key);
        if(activeRun){
            return activeRun;
        }

        const targets = ids.map(id => document.getElementById(id));
        if(targets.some(target => !target)){
            throw new Error(`Dashboard state element was not found: ${ids.join(", ")}`);
        }

        targets.forEach(target => this.showLoading(target, label));

        const run = (async()=>{
            try{
                await load();
                targets.forEach(target => this.showSuccess(target));
            }catch(error){
                console.error(`Dashboard section load failed (${ids.join(", ")}):`, error);
                targets.forEach(target => this.showError(target, ids, load, label));
            }finally{
                this.activeSections.delete(key);
            }
        })();

        this.activeSections.set(key, run);
        return run;
    },

    showLoading(target, label){
        target.hidden = false;
        target.classList.remove("dashboard-state--error");
        target.classList.add("dashboard-state", "dashboard-state--loading");
        target.setAttribute("aria-busy", "true");
        target.replaceChildren();

        const spinner = document.createElement("span");
        spinner.className = "dashboard-spinner";
        spinner.setAttribute("aria-hidden", "true");

        const text = document.createElement("span");
        text.textContent = label;
        target.append(spinner, text);
    },

    showSuccess(target){
        target.replaceChildren();
        target.hidden = true;
        target.classList.remove("dashboard-state--loading", "dashboard-state--error");
        target.classList.add("dashboard-state");
        target.setAttribute("aria-busy", "false");
    },

    showError(target, ids, load, label){
        target.hidden = false;
        target.classList.remove("dashboard-state--loading");
        target.classList.add("dashboard-state", "dashboard-state--error");
        target.setAttribute("aria-busy", "false");
        target.replaceChildren();

        const message = document.createElement("span");
        message.textContent = "Could not load this section.";

        const retry = document.createElement("button");
        retry.type = "button";
        retry.className = "dashboard-retry";
        retry.textContent = "Retry";
        retry.addEventListener("click", () => {
            this.runSection(ids, load, label);
        }, { once: true });

        target.append(message, retry);
    }
};
