/**
 * Proyecto: Fuego Lento (basado en Restaurantly - BootstrapMade)
 * Enfoque: Fase 2 (HTML + CSS) - Maquetación sin backend.
 */

(function () {
  "use strict";

  /**
   * Apply .scrolled class to the body as the page is scrolled down
   */
  function toggleScrolled() {
    const body = document.querySelector("body");
    const header = document.querySelector("#header");
    if (!header) return;

    // Only apply if header uses fixed/sticky behavior
    if (
      !header.classList.contains("scroll-up-sticky") &&
      !header.classList.contains("sticky-top") &&
      !header.classList.contains("fixed-top")
    ) {
      return;
    }

    window.scrollY > 100 ? body.classList.add("scrolled") : body.classList.remove("scrolled");
  }

  document.addEventListener("scroll", toggleScrolled);
  window.addEventListener("load", toggleScrolled);

  /**
   * Mobile nav toggle
   */
  const mobileNavToggleBtn = document.querySelector(".mobile-nav-toggle");

  function mobileNavToggle() {
    document.body.classList.toggle("mobile-nav-active");
    mobileNavToggleBtn.classList.toggle("bi-list");
    mobileNavToggleBtn.classList.toggle("bi-x");
  }

  if (mobileNavToggleBtn) {
    mobileNavToggleBtn.addEventListener("click", mobileNavToggle);
  }

  /**
   * Hide mobile nav on same-page/hash links
   */
  document.querySelectorAll("#navmenu a").forEach((navLink) => {
    navLink.addEventListener("click", () => {
      if (document.body.classList.contains("mobile-nav-active")) {
        mobileNavToggle();
      }
    });
  });

  /**
   * Preloader
   */
  const preloader = document.querySelector("#preloader");
  if (preloader) {
    window.addEventListener("load", () => {
      preloader.remove();
    });
  }

  /**
   * Scroll top button
   */
  const scrollTop = document.querySelector(".scroll-top");

  function toggleScrollTop() {
    if (!scrollTop) return;
    window.scrollY > 100 ? scrollTop.classList.add("active") : scrollTop.classList.remove("active");
  }

  if (scrollTop) {
    scrollTop.addEventListener("click", (e) => {
      e.preventDefault();
      window.scrollTo({
        top: 0,
        behavior: "smooth",
      });
    });

    window.addEventListener("load", toggleScrollTop);
    document.addEventListener("scroll", toggleScrollTop);
  }

  /**
   * AOS init (Animation on scroll)
   */
  function aosInit() {
    if (typeof AOS === "undefined") return;
    AOS.init({
      duration: 600,
      easing: "ease-in-out",
      once: true,
      mirror: false,
    });
  }
  window.addEventListener("load", aosInit);

  /**
   * GLightbox init (Gallery)
   */
  if (typeof GLightbox !== "undefined") {
    GLightbox({
      selector: ".glightbox",
    });
  }

  /**
   * Isotope layout and filters (Menu)
   */
  const isotopeLayouts = document.querySelectorAll(".isotope-layout");

  isotopeLayouts.forEach((isotopeItem) => {
    const container = isotopeItem.querySelector(".isotope-container");
    if (!container) return;

    const layout = isotopeItem.getAttribute("data-layout") || "masonry";
    const filter = isotopeItem.getAttribute("data-default-filter") || "*";
    const sort = isotopeItem.getAttribute("data-sort") || "original-order";

    let initIsotope = null;

    if (typeof imagesLoaded === "undefined" || typeof Isotope === "undefined") return;

    imagesLoaded(container, function () {
      initIsotope = new Isotope(container, {
        itemSelector: ".isotope-item",
        layoutMode: layout,
        filter: filter,
        sortBy: sort,
      });
    });

    isotopeItem.querySelectorAll(".isotope-filters li").forEach((filterBtn) => {
      filterBtn.addEventListener("click", function () {
        const active = isotopeItem.querySelector(".isotope-filters .filter-active");
        if (active) active.classList.remove("filter-active");
        this.classList.add("filter-active");

        if (initIsotope) {
          initIsotope.arrange({
            filter: this.getAttribute("data-filter"),
          });
        }

        aosInit();
      });
    });
  });

  /**
   * Correct scrolling position upon page load for URLs containing hash links.
   */
  window.addEventListener("load", function () {
    if (!window.location.hash) return;

    const section = document.querySelector(window.location.hash);
    if (!section) return;

    setTimeout(() => {
      const scrollMarginTop = getComputedStyle(section).scrollMarginTop;
      window.scrollTo({
        top: section.offsetTop - parseInt(scrollMarginTop || "0", 10),
        behavior: "smooth",
      });
    }, 100);
  });

  /**
   * Navmenu Scrollspy
   */
  const navMenuLinks = document.querySelectorAll(".navmenu a");

  function navmenuScrollspy() {
    navMenuLinks.forEach((link) => {
      if (!link.hash) return;

      const section = document.querySelector(link.hash);
      if (!section) return;

      const position = window.scrollY + 200;
      const inSection =
        position >= section.offsetTop && position <= section.offsetTop + section.offsetHeight;

      if (inSection) {
        document.querySelectorAll(".navmenu a.active").forEach((a) => a.classList.remove("active"));
        link.classList.add("active");
      } else {
        link.classList.remove("active");
      }
    });
  }

  window.addEventListener("load", navmenuScrollspy);
  document.addEventListener("scroll", navmenuScrollspy);
})();
