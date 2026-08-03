javascript:(function(){
  'use strict';
  const FIRST = 'asiaflix.org';
  const BLOCKED = [
    'appsgeyser','doubleclick','googlesyndication','googleadservices','adservice.google',
    'admob','appodeal','applovin','adcolony','ironsrc','ironsource','unityads',
    'pangle','amazon-adsystem','criteo','taboola','outbrain','pubmatic',
    'rubiconproject','openx','smartadserver','casalemedia','adform',
    'scorecardresearch','yieldmo','media.net','mgid','revcontent','adskeeper',
    'bidvertiser','admaven','adsterra','monetag','propellerads','popads',
    'popcash','clickadu','exoclick','trafficjunky','hilltopads','richads',
    'pushground','zeropark','go2cloud','offerstrack','onclick','adnxs',
    'adsrvr','bidswitch','contextweb','lijit','smaato','zedo','quantserve',
    'crwdcntrl','imrworldwide','effectivegatecpm','highperformanceformat',
    'partners.xm','xmglobal','xmza.com','xm.com','trading-point'
  ];
  const BLOCKED_PATHS = [
    'affiliate_tracking','affid=','xm-ultra-low','xm_ultra_low',
    'low-cost-trading','low_cost_trading','islamic-account-available',
    '/advert/','/advertisement/','/popunder/','onclickalgo'
  ];
  const XM_TEXT = /(xm\s*ultra\s*low|low[-\s]*cost\s+trading|islamic\s+account\s+available|your\s+capital\s+is\s+at\s+risk|start\s+now\s*xm|\bxm\s+trading\b)/i;
  const SCAM_TEXT = /(android\s+security|security\s+alert|wiretapping|your\s+android\s+has\s+been\s+hacked|virus\s+detected|run\s+a\s+test|google\s+security\s+support|install\s+(the\s+)?app|buy\s+premium|appsgeyser)/i;
  const AD_CLASS = /(^|[\s_-])(ad|ads|advert|advertisement|sponsor|sponsored|promo|promotion|banner)([\s_-]|$)/i;

  const parse = function(value) {
    try { return new URL(String(value || ''), location.href); } catch(e) { return null; }
  };
  const firstParty = function(value) {
    const u = parse(value);
    return !!u && (u.protocol === 'https:' || u.protocol === 'http:') &&
      (u.hostname === FIRST || u.hostname.endsWith('.' + FIRST));
  };
  const blockedUrl = function(value) {
    const u = parse(value);
    if (!u) return true;
    if (u.protocol === 'data:' || u.protocol === 'blob:' || u.protocol === 'about:') return false;
    if (u.protocol !== 'https:' && u.protocol !== 'http:') return true;
    const full = u.href.toLowerCase();
    const host = u.hostname.toLowerCase();
    return BLOCKED.some(function(token){ return host.indexOf(token) !== -1; }) ||
      BLOCKED_PATHS.some(function(token){ return full.indexOf(token) !== -1; });
  };
  const visible = function(node) {
    if (!node || !node.isConnected) return false;
    const style = getComputedStyle(node);
    if (style.display === 'none' || style.visibility === 'hidden' || Number(style.opacity) < 0.05) return false;
    const r = node.getBoundingClientRect();
    return r.width > 2 && r.height > 2;
  };
  const descriptor = function(node) {
    if (!node) return '';
    const attrs = ['src','href','data-src','data-lazy-src','alt','title','aria-label','class','id'];
    let out = '';
    attrs.forEach(function(name){ try { out += ' ' + (node.getAttribute(name) || ''); } catch(e) {} });
    try { out += ' ' + (node.innerText || node.textContent || '').slice(0, 900); } catch(e) {}
    return out;
  };
  const removeContainer = function(node) {
    if (!node || !node.isConnected) return;
    let target = node;
    if (target.tagName === 'IMG' || target.tagName === 'PICTURE' || target.tagName === 'SOURCE') {
      if (target.closest('a')) target = target.closest('a');
    }
    for (let i = 0; i < 3 && target.parentElement && target.parentElement !== document.body; i++) {
      const parent = target.parentElement;
      const r = parent.getBoundingClientRect();
      const text = descriptor(parent);
      const classId = String(parent.className || '') + ' ' + String(parent.id || '');
      const childCount = parent.children ? parent.children.length : 0;
      if (XM_TEXT.test(text) || SCAM_TEXT.test(text) || AD_CLASS.test(classId) ||
          (childCount <= 3 && r.height > 30 && r.height < Math.max(420, innerHeight * 0.48))) {
        target = parent;
      } else {
        break;
      }
    }
    if (target !== document.body && target !== document.documentElement) target.remove();
  };

  try { window.open = function(){ return null; }; } catch(e) {}
  try { window.showModalDialog = function(){ return null; }; } catch(e) {}
  try {
    if ('Notification' in window) {
      Notification.requestPermission = function(){ return Promise.resolve('denied'); };
    }
  } catch(e) {}

  const style = document.getElementById('asiaflix-clean-style') || document.createElement('style');
  style.id = 'asiaflix-clean-style';
  style.textContent = `
    html, body { margin: 0 !important; padding: 0 !important; min-width: 100% !important; min-height: 100% !important; }
    .adsbygoogle, ins.adsbygoogle, [data-ad-client], [data-ad-slot], [id^="google_ads"],
    [class~="ad-banner"], [class~="ad-container"], [class~="ad-wrapper"],
    [class~="advert"], [class~="advertisement"], [class~="sponsored"],
    [id~="ad-banner"], [id~="ad-container"], [id~="ad-wrapper"],
    [class*="appsgeyser"], [id*="appsgeyser"], [aria-label*="advertisement" i],
    iframe[src*="doubleclick"], iframe[src*="googlesyndication"],
    iframe[src*="xm.com"], iframe[src*="partners.xm"],
    a[href*="xm.com"], a[href*="partners.xm"] {
      display: none !important; visibility: hidden !important; width: 0 !important;
      height: 0 !important; min-height: 0 !important; margin: 0 !important; padding: 0 !important;
    }
    a:focus, button:focus, input:focus, select:focus, textarea:focus,
    summary:focus, [role="button"]:focus, [tabindex]:focus,
    .asiaflix-tv-focus {
      outline: 4px solid #00e5ff !important;
      outline-offset: 4px !important;
      box-shadow: 0 0 0 3px rgba(0,0,0,.9), 0 0 22px 8px rgba(0,229,255,.95) !important;
      border-radius: 8px !important;
      filter: brightness(1.16) contrast(1.06) !important;
      position: relative !important;
      z-index: 2147483000 !important;
      scroll-margin: 110px !important;
    }
    video:fullscreen, iframe:fullscreen, video:-webkit-full-screen, iframe:-webkit-full-screen {
      position: fixed !important; inset: 0 !important; width: 100vw !important; height: 100vh !important;
      max-width: none !important; max-height: none !important; margin: 0 !important; padding: 0 !important;
      object-fit: contain !important; background: #000 !important; z-index: 2147483647 !important;
    }
  `;
  if (!style.isConnected) (document.head || document.documentElement).appendChild(style);

  const removeAds = function(){
    const selectors = [
      '.adsbygoogle','ins.adsbygoogle','[data-ad-client]','[data-ad-slot]','[id^="google_ads"]',
      '[class~="ad-banner"]','[class~="ad-container"]','[class~="ad-wrapper"]',
      '[class~="advert"]','[class~="advertisement"]','[class~="sponsored"]',
      '[id~="ad-banner"]','[id~="ad-container"]','[id~="ad-wrapper"]',
      '[class*="appsgeyser"]','[id*="appsgeyser"]','[aria-label*="advertisement" i]',
      'iframe[src*="doubleclick"]','iframe[src*="googlesyndication"]',
      'iframe[src*="adsterra"]','iframe[src*="monetag"]','iframe[src*="propellerads"]',
      'iframe[src*="popads"]','iframe[src*="popcash"]','iframe[src*="clickadu"]',
      'iframe[src*="xm.com"]','iframe[src*="partners.xm"]',
      'a[href*="xm.com"]','a[href*="partners.xm"]'
    ];
    selectors.forEach(function(selector){
      try { document.querySelectorAll(selector).forEach(removeContainer); } catch(e) {}
    });

    document.querySelectorAll('img,picture,iframe,a,ins,aside,section,div').forEach(function(node){
      if (!node.isConnected) return;
      const desc = descriptor(node);
      if (XM_TEXT.test(desc) || SCAM_TEXT.test(desc)) {
        removeContainer(node);
        return;
      }
      const classId = String(node.className || '') + ' ' + String(node.id || '');
      if (AD_CLASS.test(classId) && node !== document.body && node !== document.documentElement) {
        const r = node.getBoundingClientRect();
        if (r.height < Math.max(500, innerHeight * 0.55)) removeContainer(node);
        return;
      }
      if (node.tagName === 'IMG' || node.tagName === 'PICTURE') {
        const r = node.getBoundingClientRect();
        const ratio = r.height > 0 ? r.width / r.height : 0;
        const parentLink = node.closest('a');
        const externalLink = parentLink && parentLink.href && !firstParty(parentLink.href);
        const nearTop = r.top < Math.max(620, innerHeight * 0.72);
        const wideBanner = r.width >= Math.max(520, innerWidth * 0.62) &&
          r.height >= 45 && r.height <= 360 && ratio >= 3.6;
        if (wideBanner && (externalLink || nearTop)) removeContainer(node);
      }
    });

    document.querySelectorAll('a,area,form').forEach(function(node){
      const value = node.href || node.action || node.getAttribute('href') || node.getAttribute('action') || '';
      if (!value) return;
      const lower = String(value).trim().toLowerCase();
      if (lower.startsWith('#') || lower.startsWith('javascript:')) {
        node.removeAttribute('target');
        return;
      }
      if (!firstParty(value)) {
        node.removeAttribute('target');
        node.setAttribute('tabindex', '-1');
        node.addEventListener('click', function(e){ e.preventDefault(); e.stopImmediatePropagation(); }, true);
      } else {
        node.removeAttribute('target');
        if (node.tagName !== 'FORM') node.setAttribute('target', '_self');
      }
    });

    document.querySelectorAll('iframe').forEach(function(frame){
      const src = frame.getAttribute('src') || '';
      if (src && blockedUrl(src)) {
        removeContainer(frame);
        return;
      }
      if (src && !firstParty(src)) {
        frame.setAttribute('sandbox', 'allow-scripts allow-same-origin allow-forms allow-presentation allow-orientation-lock');
        frame.setAttribute('allow', 'autoplay; fullscreen; encrypted-media; picture-in-picture');
        frame.setAttribute('allowfullscreen', 'true');
      }
    });
  };

  const focusSelector = 'a[href],button,input,select,textarea,summary,[role="button"],[onclick],video,iframe,[tabindex]';
  const focusables = function(){
    const list = [];
    document.querySelectorAll(focusSelector).forEach(function(node){
      if (!visible(node) || node.disabled || node.getAttribute('aria-hidden') === 'true') return;
      if (node.tagName === 'A' && node.href && !firstParty(node.href)) return;
      if (node.getAttribute('tabindex') === '-1') return;
      if (!node.hasAttribute('tabindex')) node.setAttribute('tabindex', '0');
      list.push(node);
    });
    return list;
  };
  const markFocus = function(node){
    document.querySelectorAll('.asiaflix-tv-focus').forEach(function(old){ old.classList.remove('asiaflix-tv-focus'); });
    if (!node) return;
    node.classList.add('asiaflix-tv-focus');
    try { node.focus({preventScroll:true}); } catch(e) { try { node.focus(); } catch(x) {} }
    try { node.scrollIntoView({behavior:'smooth', block:'center', inline:'center'}); } catch(e) {}
  };
  const center = function(node){
    const r = node.getBoundingClientRect();
    return {x:r.left + r.width / 2, y:r.top + r.height / 2, r:r};
  };
  window.__asiaFlixTvMove = function(direction){
    removeAds();
    const list = focusables();
    if (!list.length) return false;
    let current = document.activeElement;
    if (!current || current === document.body || !list.includes(current) || !visible(current)) {
      markFocus(list[0]);
      return true;
    }
    const a = center(current);
    let best = null;
    let bestScore = Number.POSITIVE_INFINITY;
    list.forEach(function(candidate){
      if (candidate === current) return;
      const b = center(candidate);
      const dx = b.x - a.x;
      const dy = b.y - a.y;
      let primary = 0;
      let cross = 0;
      if (direction === 'left') { if (dx >= -2) return; primary = -dx; cross = Math.abs(dy); }
      if (direction === 'right') { if (dx <= 2) return; primary = dx; cross = Math.abs(dy); }
      if (direction === 'up') { if (dy >= -2) return; primary = -dy; cross = Math.abs(dx); }
      if (direction === 'down') { if (dy <= 2) return; primary = dy; cross = Math.abs(dx); }
      const overlapBonus = cross < Math.max(a.r.width, a.r.height) * 0.7 ? -120 : 0;
      const score = primary + cross * 2.35 + overlapBonus;
      if (score < bestScore) { bestScore = score; best = candidate; }
    });
    if (best) {
      markFocus(best);
      return true;
    }
    const amount = Math.round((direction === 'up' || direction === 'down' ? innerHeight : innerWidth) * 0.72);
    if (direction === 'up') scrollBy({top:-amount, behavior:'smooth'});
    if (direction === 'down') scrollBy({top:amount, behavior:'smooth'});
    if (direction === 'left') scrollBy({left:-amount, behavior:'smooth'});
    if (direction === 'right') scrollBy({left:amount, behavior:'smooth'});
    return true;
  };
  window.__asiaFlixTvActivate = function(){
    removeAds();
    const node = document.activeElement;
    if (!node || node === document.body) {
      const list = focusables();
      if (list.length) markFocus(list[0]);
      return true;
    }
    try {
      if (node.tagName === 'VIDEO') {
        if (node.paused) node.play(); else node.pause();
        if (!document.fullscreenElement && node.requestFullscreen) node.requestFullscreen().catch(function(){});
        return true;
      }
      if (node.tagName === 'IFRAME') {
        if (!document.fullscreenElement && node.requestFullscreen) node.requestFullscreen().catch(function(){});
        return true;
      }
      node.click();
      return true;
    } catch(e) { return false; }
  };
  window.__asiaFlixTvHome = function(){ scrollTo({top:0, behavior:'smooth'}); return true; };

  document.addEventListener('focusin', function(event){
    if (event.target && event.target.classList) {
      document.querySelectorAll('.asiaflix-tv-focus').forEach(function(old){ if (old !== event.target) old.classList.remove('asiaflix-tv-focus'); });
      event.target.classList.add('asiaflix-tv-focus');
    }
  }, true);
  document.addEventListener('click', function(event){
    const target = event.target && event.target.closest ? event.target.closest('a,area') : null;
    if (!target) return;
    const raw = target.getAttribute('href') || '';
    const lower = raw.trim().toLowerCase();
    if (!raw || lower.startsWith('#') || lower.startsWith('javascript:')) {
      target.removeAttribute('target');
      return;
    }
    if (!firstParty(raw)) {
      event.preventDefault();
      event.stopImmediatePropagation();
      return false;
    }
    target.removeAttribute('target');
    target.setAttribute('target', '_self');
  }, true);
  document.addEventListener('submit', function(event){
    const form = event.target;
    if (!form || !form.action) return;
    if (!firstParty(form.action)) {
      event.preventDefault();
      event.stopImmediatePropagation();
    } else {
      form.removeAttribute('target');
    }
  }, true);
  document.addEventListener('play', function(event){
    const video = event.target;
    if (!video || video.tagName !== 'VIDEO' || document.fullscreenElement) return;
    try {
      const result = video.requestFullscreen ? video.requestFullscreen() : null;
      if (result && result.catch) result.catch(function(){});
    } catch(e) {}
  }, true);

  if (!window.__asiaFlixObserver) {
    let timer = 0;
    window.__asiaFlixObserver = new MutationObserver(function(){
      clearTimeout(timer);
      timer = setTimeout(function(){ removeAds(); focusables(); }, 220);
    });
    window.__asiaFlixObserver.observe(document.documentElement, {
      childList:true, subtree:true, attributes:true,
      attributeFilter:['href','src','target','action','class','id','style','alt','title']
    });
  }
  removeAds();
  focusables();
  if (!document.activeElement || document.activeElement === document.body) {
    const initial = focusables()[0];
    if (initial) markFocus(initial);
  }
})();
