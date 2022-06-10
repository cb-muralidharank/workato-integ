(() => {
    "use strict";
    var t = function () {
        return t = Object.assign || function (t) {
            for (var e, n = 1, r = arguments.length; n < r; n++) for (var o in e = arguments[n]) Object.prototype.hasOwnProperty.call(e, o) && (t[o] = e[o]);
            return t
        }, t.apply(this, arguments)
    }, e = /^[a-z0-9+\-.]+:\/\//, n = function () {
        function n(t) {
            this.config = t
        }

        return n.prototype.updateConfig = function (e) {
            this.config = t(t({}, this.config), e)
        }, n.prototype.getWorkatoUrl = function (t, n) {
            void 0 === n && (n = !1);
            var r = this.config, o = r.vendorOrigin, i = r.pathPrefix;
            if (e.test(t)) {
                if (0 !== t.indexOf(o)) return null;
                t = t.slice(o.length)
            }
            if (0 !== t.indexOf(i) || "/" !== t.charAt(i.length) && t.length !== i.length) return null;
            var a = t.slice(i.length);
            return "/" !== a.charAt(0) && (a = "/".concat(a)), n && (a = "".concat(this.config.workatoOrigin).concat(a)), a
        }, n.prototype.getEmbeddingUrl = function (t, n) {
            void 0 === n && (n = !1);
            var r = this.config.workatoOrigin;
            if (e.test(t)) {
                if (0 !== t.indexOf(r)) return null;
                t = t.slice(r.length)
            }
            "/" !== t.charAt(0) && (t = "/".concat(t));
            var o = "".concat(this.config.pathPrefix).concat(t);
            return n && (o = "".concat(this.config.vendorOrigin).concat(o)), o
        }, n
    }(), r = function () {
        function t(t) {
            var e = this;
            this.config = t, this.currentWorkatoUrl = null, this.configured = !1, this.pathPrefix = "/", this.messageHandlers = Object.create(null), this.pendingMessages = [], this.handleMessageEvent = function (t) {
                var n = t.source;
                if (t.origin === e.config.sourceOrigin && n && n.parent === window) {
                    var r = e.processMessage(t.data);
                    if (r) {
                        switch (r.type) {
                            case"loaded":
                                e.sourceIframe = n, e.currentWorkatoUrl = r.payload.url;
                                var o = e.pendingMessages;
                                e.pendingMessages = [], o.forEach((function (t) {
                                    return e.send(t)
                                }));
                                break;
                            case"navigated":
                                e.currentWorkatoUrl = r.payload.url;
                                break;
                            case"unloaded":
                                e.sourceIframe = null, e.currentWorkatoUrl = null
                        }
                        e.emit(r.type, r.payload), e.emit("*", r)
                    }
                }
            }, this.utils = new n({
                workatoOrigin: t.sourceOrigin,
                vendorOrigin: location.origin,
                pathPrefix: this.pathPrefix
            }), window.addEventListener("message", this.handleMessageEvent)
        }

        return Object.defineProperty(t.prototype, "loaded", {
            get: function () {
                return Boolean(this.sourceIframe)
            }, enumerable: !1, configurable: !0
        }), t.prototype.configure = function (t) {
            return this.pathPrefix = t.embeddingUrlPrefix, this.utils.updateConfig({pathPrefix: this.pathPrefix}), this
        }, t.prototype.send = function (t) {
            return this.loaded ? this.sourceIframe.postMessage(JSON.stringify(t), this.config.sourceOrigin) : (this.pendingMessages = this.pendingMessages.filter((function (e) {
                var n = e.type;
                return t.type !== n
            })), this.pendingMessages.push(t)), this
        }, t.prototype.on = function (t, e) {
            return this.messageHandlers[t] || (this.messageHandlers[t] = []), this.messageHandlers[t].push(e), this
        }, t.prototype.off = function (t, e) {
            var n = this.messageHandlers[t];
            if (null == n ? void 0 : n.length) {
                var r = n.indexOf(e);
                r >= 0 && n.splice(r, 1)
            }
            return this
        }, t.prototype.navigateTo = function (t) {
            return t !== this.currentWorkatoUrl && this.send({type: "navigation", payload: {url: t}}), this
        }, t.prototype.handleNavigation = function (t) {
            var e = this;
            void 0 === t && (t = {}), this.disableNavigationHandling();
            var n = this;
            this.on("loaded", o).on("navigated", i), window.addEventListener("popstate", s);
            var r = this.addLinkClickHandler((function (e, r) {
                var o = e.pathname + e.search + e.hash, i = n.extractWorkatoUrl(o);
                if (i && o !== n.currentVendorUrl) {
                    var a = !0, s = !0;
                    t.onVendorNavigation && t.onVendorNavigation({
                        reason: "link",
                        embeddingUrl: o,
                        workatoUrl: i,
                        link: e,
                        event: r,
                        preventVendorUrlChange: function () {
                            s = !1
                        },
                        preventWorkatoUrlChange: function () {
                            a = !1
                        }
                    }), s && (r.preventDefault(), history.pushState(null, document.title, o)), a && (r.preventDefault(), n.loaded ? n.navigateTo(i) : s ? location.reload() : location.href = o)
                }
            }));
            return this.disableNavigationHandler = function () {
                e.off("loaded", o).off("navigated", i), window.removeEventListener("popstate", s), r()
            }, this;

            function o(t) {
                a(t.url, !0)
            }

            function i(t) {
                a(t.url, t.replaced)
            }

            function a(e, r) {
                var o = n.constructEmbeddingUrl(e);
                if (n.currentVendorUrl !== o) {
                    var i = !0;
                    t.onWorkatoNavigation && t.onWorkatoNavigation({
                        workatoUrl: e,
                        embeddingUrl: o,
                        urlReplaced: r,
                        preventVendorUrlChange: function () {
                            i = !1
                        }
                    }), i && (r ? history.replaceState(null, document.title, o) : history.pushState(null, document.title, o))
                }
            }

            function s() {
                var e = n.extractWorkatoUrl(n.currentVendorUrl);
                if (null !== e) {
                    var r = !0;
                    t.onVendorNavigation && t.onVendorNavigation({
                        reason: "history",
                        embeddingUrl: n.currentVendorUrl,
                        workatoUrl: e,
                        preventWorkatoUrlChange: function () {
                            r = !1
                        }
                    }), r && (n.loaded ? n.navigateTo(e) : location.reload())
                }
            }
        }, t.prototype.disableNavigationHandling = function () {
            return this.disableNavigationHandler && (this.disableNavigationHandler(), this.disableNavigationHandler = null), this
        }, t.prototype.generateIFrameUrl = function (t, e) {
            return "string" != typeof e && null === (e = this.extractWorkatoUrl(this.currentVendorUrl)) && this.throwError('"'.concat(this.currentVendorUrl, '" is not valid embedding URL - it must start with "').concat(this.pathPrefix, '"')), "".concat(this.config.sourceOrigin, "/direct_link?workato_dl_path=").concat(encodeURIComponent(e), "&workato_dl_token=").concat(encodeURIComponent(t))
        }, t.prototype.extractWorkatoUrl = function (t) {
            return this.utils.getWorkatoUrl(t)
        }, t.prototype.constructEmbeddingUrl = function (t) {
            return this.utils.getEmbeddingUrl(t)
        }, Object.defineProperty(t.prototype, "currentVendorUrl", {
            get: function () {
                return location.pathname + location.search + location.hash
            }, enumerable: !1, configurable: !0
        }), t.prototype.addLinkClickHandler = function (t) {
            var e = this;
            return document.addEventListener("click", n), function () {
                document.removeEventListener("click", n)
            };

            function n(n) {
                var r = e.getClosestLink(n.target);
                if (!(!r || r.origin !== location.origin || 0 !== n.button || n.shiftKey || n.metaKey || n.ctrlKey || n.altKey)) {
                    var o = r.getAttribute("target");
                    o && "_self" !== o || t(r, n)
                }
            }
        }, t.prototype.emit = function (t, e) {
            (this.messageHandlers[t] || []).forEach((function (t) {
                return t(e)
            }))
        }, t.prototype.processMessage = function (t) {
            if ("string" != typeof t) return null;
            try {
                t = JSON.parse(t)
            } catch (t) {
                return null
            }
            return this.isWorkatoMessage(t) ? (delete t.wk, t) : null
        }, t.prototype.getClosestLink = function (t) {
            do {
                if ("A" === t.tagName) return t;
                t = t.parentElement
            } while (t);
            return null
        }, t.prototype.throwError = function (t, e) {
            throw void 0 === e && (e = Error), new e("WorkatoEmbeddingClient: ".concat(t))
        }, t.prototype.isWorkatoMessage = function (t) {
            return Boolean(t && !0 === t.wk)
        }, t
    }();
    window.Workato = new r({sourceOrigin: "https://app.workato.com"})
})();
//# sourceMappingURL=https://app.workato.com/sourcemaps/embedding-client.js.map