(function() {
    'use strict';

    // Only enhancement: Native lazy loading fallback for older browsers
    // For browsers that don't support loading="lazy" attribute
    if ('IntersectionObserver' in window) {
        // Find images that need lazy loading
        const images = document.querySelectorAll('img[loading="lazy"]');

        // Only apply if the browser doesn't support native lazy loading
        if ('loading' in HTMLImageElement.prototype) {
            // Browser supports native lazy loading, do nothing
            return;
        }

        // Fallback for browsers without native lazy loading
        const imageObserver = new IntersectionObserver(function(entries, observer) {
            entries.forEach(function(entry) {
                if (entry.isIntersecting) {
                    const img = entry.target;
                    // Only load if we have a data-src attribute (for fallback)
                    if (img.dataset.src) {
                        img.src = img.dataset.src;
                        img.removeAttribute('data-src');
                    }
                    observer.unobserve(img);
                }
            });
        }, {
            // Start loading when the image is 50 px away from the viewport
            rootMargin: '50px'
        });

        // Observe all lazy images
        images.forEach(function(img) {
            // Store original src in data-src for fallback
            if (!img.dataset.src && img.src) {
                img.dataset.src = img.src;
                img.removeAttribute('src');
            }
            imageObserver.observe(img);
        });
    }
})();