(function () {
    const queue = [];
    let activePopup = null;
    let dialog;
    let previousFocus;

    const modules = {
        "bill-history": { name: "Bill", area: "Billing" },
        billing: { name: "Bill", area: "Billing" },
        "company-master": { name: "Company", area: "Company Master" },
        customer: { name: "Customer", area: "Customer" },
        "daily-report": { name: "Report", area: "Reports" },
        "expiry-breakage": { name: "Inventory entry", area: "Inventory" },
        "gst-master": { name: "GST record", area: "GST" },
        inventory: { name: "Medicine stock", area: "Inventory" },
        "medicine": { name: "Medicine", area: "Medicine Master" },
        "movement-report": { name: "Report", area: "Reports" },
        "opening-stock": { name: "Opening stock", area: "Inventory" },
        outstanding: { name: "Supplier balance", area: "Reports" },
        payment: { name: "Payment", area: "Payments" },
        "payment-history": { name: "Payment", area: "Payments" },
        "purchase-list": { name: "Purchase", area: "Purchase" },
        "purchase-return": { name: "Purchase return", area: "Purchase" },
        purchase: { name: "Purchase", area: "Purchase" },
        "sale-return": { name: "Sale return", area: "Billing" },
        "schedule-master": { name: "Schedule", area: "Schedule Master" },
        supplier: { name: "Supplier", area: "Supplier" }
    };

    function currentModule() {
        const page = window.location.pathname
            .split("/")
            .pop()
            .replace(/\.html?$/i, "")
            .toLowerCase();

        if (page === "login") {
            return { name: "Sign-in", area: "Account" };
        }

        return modules[page] || { name: "Record", area: "Medical Store" };
    }

    function classifyMessage(message) {
        const text = String(message);

        if (/invalid credentials|failed|failure|error|unable|could not|out of stock|expired|insufficient stock|available stock|exceeds? available|delete failed/i.test(text)) {
            return { type: "error", icon: "\u00d7" };
        }

        if (/saved|deleted|added|updated|success|complete/i.test(text)) {
            return { type: "success", icon: "\u2713" };
        }

        if (/required|select|enter|invalid|must be|please|no customer|no bill/i.test(text)) {
            return { type: "warning", icon: "!" };
        }

        return { type: "info", icon: "i" };
    }

    function getSuccessTitle(message, module) {
        const text = String(message);
        const entity = titleCase(module.name === "GST record" ? "GST" : module.name);

        if (/deleted/i.test(text)) return entity + " Deleted Successfully";
        if (/updated/i.test(text)) return entity + " Updated Successfully";
        if (/added/i.test(text)) return entity + " Added Successfully";
        if (/saved/i.test(text)) return entity + " Saved Successfully";
        return entity + " Completed Successfully";
    }

    function getSuccessMessage(message, module) {
        const text = String(message);

        if (/deleted/i.test(text)) {
            return "The " + module.name.toLowerCase() + " has been deleted.";
        }
        if (/updated/i.test(text)) {
            return "The " + module.name.toLowerCase() + " details have been updated.";
        }
        if (/medicine saved/i.test(text)) {
            return "The medicine details have been saved to your inventory.";
        }
        if (/bill generated successfully/i.test(text)) {
            return text;
        }
        if (/customer/i.test(text)) {
            return "The customer details have been saved.";
        }
        if (/supplier/i.test(text)) {
            return "The supplier details have been saved.";
        }
        if (/purchase return/i.test(text)) {
            return "The purchase return has been recorded successfully.";
        }
        if (/sale return/i.test(text)) {
            return "The sale return has been recorded successfully.";
        }
        if (/purchase/i.test(text)) {
            return "The purchase and received stock have been saved.";
        }
        if (/opening stock|stock added/i.test(text)) {
            return "The opening stock has been added to inventory.";
        }
        if (/payment/i.test(text)) {
            return "The payment has been recorded successfully.";
        }
        if (/gst/i.test(text)) {
            return "The GST details have been saved.";
        }
        if (/saved|added|updated/i.test(text)) {
            return "The " + module.name.toLowerCase() + " details have been saved successfully.";
        }
        return text;
    }

    function getAlertContent(message) {
        const text = String(message).trim();
        const lower = text.toLowerCase();
        const module = currentModule();
        const classification = classifyMessage(text);
        let title;
        let description = text;

        if (classification.type === "success") {
            title = getSuccessTitle(text, module);
            description = getSuccessMessage(text, module);
        } else if (classification.type === "error") {
            if (/invalid credentials/i.test(text)) {
                title = "Unable to Sign In";
                description = "The username or password is incorrect. Check your credentials and try again.";
            } else if (/out of stock/i.test(text)) {
                title = "Medicine Out of Stock";
                description = "No stock is available for this medicine. Add stock before billing.";
            } else if (/expired/i.test(text)) {
                title = "Medicine Expired";
                description = "This medicine is past its expiry date and cannot be billed.";
            } else if (/available stock/i.test(text)) {
                const quantity = text.match(/available stock\s*:?\s*(\d+)/i);
                title = "Insufficient Stock";
                description = quantity
                    ? "Only " + quantity[1] + " unit(s) are available. Reduce the requested quantity and try again."
                    : "Requested quantity exceeds the available stock. Reduce the quantity and try again.";
            } else if (/delete failed/i.test(text)) {
                title = "Unable to Delete " + titleCase(module.name);
                description = "The " + module.name.toLowerCase() + " could not be deleted. Refresh the page and try again.";
            } else {
                title = "Unable to Save " + titleCase(module.name);
                description = "The " + module.name.toLowerCase() + " could not be saved. Check your connection and try again.";
            }
        } else if (classification.type === "warning") {
            if (/fill all required fields/i.test(text)) {
                title = "Complete Required Information";
                description = "Complete all required fields before saving.";
            } else if (/required/i.test(text)) {
                const field = text.replace(/^please\s+/i, "").replace(/\s+is required\.?$/i, "");
                title = module.area + " Details Required";
                description = "Enter " + field.toLowerCase() + " to continue.";
            } else if (/gst must be between/i.test(text)) {
                title = "Check GST Percentage";
                description = "Enter a GST percentage from 0% to 28%.";
            } else if (/out of range|must be between/i.test(text)) {
                title = "Check " + module.name + " Details";
                description = text.replace(/[.!]+$/, "") + ". Review the value and try again.";
            } else if (/no customer found/i.test(text)) {
                title = "Customer Not Found";
                description = "No matching customer was found. Check the name or mobile number and try again.";
            } else if (/select valid medicine/i.test(text)) {
                title = "Select a Medicine";
                description = "Choose a medicine from the search results before adding it to the bill.";
            } else {
                title = "Check " + module.area + " Details";
                description = text.replace(/^please\s+/i, "").replace(/[.!]+$/, "");
                description = description.charAt(0).toUpperCase() + description.slice(1) + " before continuing.";
            }
        } else {
            title = module.area + " Information";
        }

        return {
            type: classification.type,
            title: title,
            message: description,
            icon: classification.icon
        };
    }

    function getConfirmContent(message, options) {
        const module = currentModule();
        const deleting = /delete/i.test(String(message));
        const subject = options.subject || module.name.toLowerCase();
        const displaySubject = titleCase(options.subject || module.name);

        return {
            type: "warning",
            title: options.title || (deleting ? "Delete " + displaySubject + "?" : "Please Confirm"),
            message: options.description || (
                deleting
                    ? "Are you sure you want to delete this " + subject + "? This action may affect related records."
                    : String(message)
            ),
            icon: "!",
            confirmText: options.confirmText || (deleting ? "Delete " + displaySubject : "Continue"),
            cancelText: options.cancelText || "Cancel",
            destructive: deleting
        };
    }

    function titleCase(value) {
        return value.replace(/\b[a-z]/g, function (letter) {
            return letter.toUpperCase();
        }).replace(/\bGst\b/g, "GST");
    }

    function createElement(tagName, className, text) {
        const element = document.createElement(tagName);
        element.className = className;

        if (text !== undefined) {
            element.textContent = text;
        }

        return element;
    }

    function createDialog() {
        if (dialog) {
            return;
        }

        dialog = createElement("dialog", "app-popup");
        dialog.setAttribute("aria-labelledby", "app-popup-title");
        dialog.setAttribute("aria-describedby", "app-popup-message");

        dialog.addEventListener("cancel", function (event) {
            event.preventDefault();
            finishPopup(false);
        });

        dialog.addEventListener("click", function (event) {
            if (event.target === dialog) {
                finishPopup(false);
            }
        });

        document.body.appendChild(dialog);
    }

    function finishPopup(result) {
        if (!activePopup) {
            return;
        }

        const completedPopup = activePopup;
        activePopup = null;
        dialog.close();

        if (completedPopup.resolve) {
            completedPopup.resolve(result);
        }

        if (queue.length > 0) {
            showNextPopup();
        } else if (previousFocus && typeof previousFocus.focus === "function") {
            previousFocus.focus();
        }
    }

    function showNextPopup() {
        if (activePopup || queue.length === 0) {
            return;
        }

        createDialog();
        activePopup = queue.shift();
        previousFocus = document.activeElement;

        const content = activePopup.kind === "confirm"
            ? getConfirmContent(activePopup.message, activePopup.options)
            : getAlertContent(activePopup.message);

        if (activePopup.options && activePopup.options.type) {
            content.type = activePopup.options.type;
            content.icon = {
                success: "\u2713",
                warning: "!",
                error: "\u00d7",
                info: "i"
            }[content.type] || content.icon;
        }
        if (activePopup.options && activePopup.options.title) {
            content.title = activePopup.options.title;
        }
        if (activePopup.options && activePopup.options.description) {
            content.message = activePopup.options.description;
        }

        dialog.className = "app-popup app-popup--" + content.type;
        dialog.setAttribute("role", activePopup.kind === "confirm" ? "alertdialog" : "dialog");
        dialog.replaceChildren();

        const body = createElement("div", "app-popup__body");
        const icon = createElement("div", "app-popup__icon", content.icon);
        icon.setAttribute("aria-hidden", "true");

        const popupContent = createElement("div", "app-popup__content");
        const title = createElement("h2", "app-popup__title", content.title);
        title.id = "app-popup-title";
        const message = createElement("p", "app-popup__message", content.message);
        message.id = "app-popup-message";
        popupContent.append(title, message);
        body.append(icon, popupContent);

        const actions = createElement("div", "app-popup__actions");
        if (activePopup.kind === "confirm") {
            const cancelButton = createElement(
                "button",
                "app-popup__button app-popup__button--secondary",
                content.cancelText
            );
            cancelButton.type = "button";
            cancelButton.addEventListener("click", function () {
                finishPopup(false);
            });
            actions.appendChild(cancelButton);
        }

        const confirmClass = content.destructive
            ? "app-popup__button app-popup__button--primary app-popup__button--danger"
            : "app-popup__button app-popup__button--primary";
        const confirmButton = createElement(
            "button",
            confirmClass,
            activePopup.kind === "confirm" ? content.confirmText : "Got it"
        );
        confirmButton.type = "button";
        confirmButton.addEventListener("click", function () {
            finishPopup(true);
        });

        dialog.append(body, actions);
        actions.appendChild(confirmButton);
        dialog.showModal();
        confirmButton.focus();
    }

    function enqueue(kind, message, options) {
        return new Promise(function (resolve) {
            queue.push({
                kind: kind,
                message: message,
                options: options || {},
                resolve: resolve
            });
            showNextPopup();
        });
    }

    function notify(type, title, message) {
        return enqueue("alert", message, {
            type: type,
            title: title,
            description: message
        });
    }

    window.appPopups = {
        alert: function (message, options) {
            return enqueue("alert", message, options);
        },
        confirm: function (message, options) {
            return enqueue("confirm", message, options);
        },
        success: function (title, message) {
            return notify("success", title, message);
        },
        warning: function (title, message) {
            return notify("warning", title, message);
        },
        error: function (title, message) {
            return notify("error", title, message);
        },
        info: function (title, message) {
            return notify("info", title, message);
        }
    };

    window.alert = function (message) {
        window.appPopups.alert(message);
    };
}());
