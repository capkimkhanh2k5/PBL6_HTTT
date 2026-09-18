/** @type {import('tailwindcss').Config} */
export default {
  darkMode: "class",
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: { "surface-container-highest": "#dee3e4", "on-tertiary-container": "#f4fbff", "secondary-container": "#fb7059", "on-tertiary-fixed-variant": "#2e4b56", "on-error-container": "#93000a", "primary-fixed-dim": "#79d4e2", "primary-container": "#087f8c", "secondary": "#aa3524", "surface-container-lowest": "#ffffff", "surface-container": "#eaeff0", "tertiary-fixed": "#c9e7f6", "on-primary": "#ffffff", "outline": "#6e797b", "tertiary": "#425e6a", "surface-tint": "#006874", "inverse-surface": "#2c3132", "surface-bright": "#f5fafb", "on-primary-container": "#effdff", "on-error": "#ffffff", "on-background": "#171d1e", "tertiary-fixed-dim": "#adcbd9", "on-secondary-fixed-variant": "#891d0f", "surface-variant": "#dee3e4", "on-surface-variant": "#3e494a", "inverse-primary": "#79d4e2", "surface-container-high": "#e4e9ea", "background": "#f5fafb", "inverse-on-surface": "#edf2f3", "error-container": "#ffdad6", "surface-container-low": "#eff4f5", "on-tertiary-fixed": "#001f29", "primary": "#00646f", "secondary-fixed-dim": "#ffb4a7", "on-primary-fixed-variant": "#004f57", "on-surface": "#171d1e", "on-tertiary": "#ffffff", "surface": "#f5fafb", "on-secondary": "#ffffff", "error": "#ba1a1a", "on-secondary-container": "#6c0600", "primary-fixed": "#95f1ff", "secondary-fixed": "#ffdad4", "tertiary-container": "#5a7784", "outline-variant": "#bdc9ca", "on-secondary-fixed": "#400200", "on-primary-fixed": "#001f24", "surface-dim": "#d6dbdc" },
      borderRadius: { "DEFAULT": "0.25rem", "lg": "0.5rem", "xl": "0.75rem", "full": "9999px" },
      spacing: { "space-md": "1rem", "space-sm": "0.5rem", "margin": "5rem", "space-xs": "0.25rem", "space-lg": "1.5rem", "space-2xl": "3rem", "margin-sm": "1.5rem", "space-xl": "2rem", "space-3xl": "4.5rem", "gutter": "1.5rem", "gutter-lg": "2rem", "gutter-sm": "1rem", "margin-md": "3rem" },
      fontFamily: {
        sans: ["Plus Jakarta Sans", "sans-serif"],
        heading: ["Outfit", "sans-serif"],
        outfit: ["Outfit", "sans-serif"],
        "label-md": ["Plus Jakarta Sans", "sans-serif"],
        "headline-sm": ["Outfit", "sans-serif"],
        "label-lg": ["Plus Jakarta Sans", "sans-serif"],
        "headline-md": ["Outfit", "sans-serif"],
        "body-sm": ["Plus Jakarta Sans", "sans-serif"],
        "body-md": ["Plus Jakarta Sans", "sans-serif"],
        "display-xl": ["Outfit", "sans-serif"],
        "headline-xl": ["Outfit", "sans-serif"],
        "body-xl": ["Plus Jakarta Sans", "sans-serif"],
        "headline-lg": ["Outfit", "sans-serif"],
        "display-lg": ["Outfit", "sans-serif"],
        "body-lg": ["Plus Jakarta Sans", "sans-serif"],
        "label-sm": ["Plus Jakarta Sans", "sans-serif"],
      },
      fontSize: { "label-md": ["12px", { "lineHeight": "16px", "letterSpacing": "0.02em", "fontWeight": "600" }], "headline-sm": ["18px", { "lineHeight": "26px", "fontWeight": "600" }], "label-lg": ["14px", { "lineHeight": "20px", "letterSpacing": "0.01em", "fontWeight": "600" }], "headline-md": ["22px", { "lineHeight": "30px", "letterSpacing": "-0.01em", "fontWeight": "600" }], "body-sm": ["12px", { "lineHeight": "18px", "fontWeight": "400" }], "body-md": ["14px", { "lineHeight": "22px", "fontWeight": "400" }], "display-xl": ["56px", { "lineHeight": "64px", "letterSpacing": "-0.02em", "fontWeight": "700" }], "headline-xl": ["36px", { "lineHeight": "44px", "letterSpacing": "-0.015em", "fontWeight": "700" }], "body-xl": ["18px", { "lineHeight": "28px", "fontWeight": "400" }], "headline-lg": ["28px", { "lineHeight": "36px", "letterSpacing": "-0.01em", "fontWeight": "600" }], "display-lg": ["44px", { "lineHeight": "52px", "letterSpacing": "-0.02em", "fontWeight": "700" }], "body-lg": ["16px", { "lineHeight": "24px", "fontWeight": "400" }], "label-sm": ["11px", { "lineHeight": "14px", "letterSpacing": "0.04em", "fontWeight": "700" }] }
    },
  },
  plugins: [],
}
