(()=>{

  // ---------- Helpers ----------
  const $ = (s)=>document.querySelector(s);
  const byId = (id)=>document.getElementById(id);

  function safeTry(fn){ try{ return fn(); }catch(_e){ return null; } }

  // Make sure halos exist (boss + hero)
  function ensureRuneHalo_(){
    const heroFx = byId("heroFx");
    const bossFx = byId("bossFx");
    if(heroFx && !heroFx.querySelector(".runeHalo")){
      const h = document.createElement("div");
      h.className = "runeHalo";
      h.style.setProperty("--h", "205deg");
      heroFx.appendChild(h);
    }
    if(bossFx && !bossFx.querySelector(".runeHalo")){
      const h = document.createElement("div");
      h.className = "runeHalo";
      // boss hue gets set dynamically when stage loads
      h.style.setProperty("--h", "0deg");
      bossFx.appendChild(h);
    }
  }

  function setBossHaloHue_(deg){
    const bossFx = byId("bossFx");
    const halo = bossFx && bossFx.querySelector(".runeHalo");
    if(halo) halo.style.setProperty("--h", `${deg||0}deg`);
  }

  // Procedural "hurt" sound (for boss hits on hero)
  let _ctx = null;
  function audioCtx_(){
    if(_ctx) return _ctx;
    const AC = window.AudioContext || window.webkitAudioContext;
    if(!AC) return null;
    _ctx = new AC();
    return _ctx;
  }
  function heroHurtSfx_(){
    const ctx = audioCtx_();
    if(!ctx) return;
    // don't spam if muted
    if(window.audioBus?.isMuted) return;
    const t = ctx.currentTime;
    const o = ctx.createOscillator();
    const g = ctx.createGain();
    const f = ctx.createBiquadFilter();
    f.type = "lowpass";
    f.frequency.setValueAtTime(1100, t);
    f.frequency.exponentialRampToValueAtTime(260, t+0.18);
    o.type = "sawtooth";
    o.frequency.setValueAtTime(220, t);
    o.frequency.exponentialRampToValueAtTime(90, t+0.16);
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(0.15, t+0.02);
    g.gain.exponentialRampToValueAtTime(0.0001, t+0.20);
    o.connect(f); f.connect(g); g.connect(ctx.destination);
    o.start(t); o.stop(t+0.22);
  }

  // Spawn a traveling rune ring projectile
  function spawnRuneBolt_(fromEl, toEl, kind){
    if(!fromEl || !toEl) return;
    const a = fromEl.getBoundingClientRect();
    const b = toEl.getBoundingClientRect();
    const sx = a.left + a.width/2;
    const sy = a.top + a.height/2;
    const ex = b.left + b.width/2;
    const ey = b.top + b.height/2;

    const bolt = document.createElement("div");
    bolt.className = `runeBolt rune-${kind||"fire"}`;
    bolt.style.left = `${sx}px`;
    bolt.style.top  = `${sy}px`;
    document.body.appendChild(bolt);

    const dur = 520 + Math.random()*320;
    const kfs = [
      { transform: "translate(-50%,-50%) scale(.72) rotate(0deg)", opacity: 0.0, offset: 0 },
      { opacity: 0.95, offset: 0.08 },
      { transform: `translate(calc(-50% + ${(ex-sx)*0.55}px), calc(-50% + ${(ey-sy)*0.55}px)) scale(1.18) rotate(220deg)`, opacity: 0.95, offset: 0.62 },
      { transform: `translate(calc(-50% + ${(ex-sx)}px), calc(-50% + ${(ey-sy)}px)) scale(.86) rotate(420deg)`, opacity: 0.0, offset: 1 }
    ];
    const anim = bolt.animate(kfs, { duration: dur, easing: "cubic-bezier(.22,.9,.22,1)", fill: "forwards" });
    anim.onfinish = ()=> bolt.remove();
  }

  function hitShake_(el){
    if(!el) return;
    el.classList.remove("hitShake");
    // force reflow
    void el.offsetWidth;
    el.classList.add("hitShake");
    setTimeout(()=>el.classList.remove("hitShake"), 420);
  }

  function kindFromBoss_(boss){
    // bossAttackProfile_ exists in this build; use it if possible.
    const prof = safeTry(()=> (typeof window.bossAttackProfile_ === "function" ? window.bossAttackProfile_(boss) : null));
    const k = (prof && prof.kind) ? String(prof.kind).toLowerCase() : "";
    if(k.includes("wind")) return "wind";
    if(k.includes("water")) return "water";
    if(k.includes("dark") || k.includes("evil") || k.includes("shadow")) return "dark";
    return "fire";
  }

  // Ensure class -> sound mapping (requested)
  // We override heroAttackProfile_ to include missing classes.
  const _origHeroAttackProfile = window.heroAttackProfile_;
  window.heroAttackProfile_ = function(hero){
    const n = String((hero && (hero.name||hero.className||hero.title)) || "").toLowerCase();
    // base
    const prof = (typeof _origHeroAttackProfile === "function") ? _origHeroAttackProfile(hero) : { kind:"sword", gif:"atk_sword_slash", sound:"sword_slash_sound_effect_no_copyright" };

    // Requested mappings
    if(n.includes("valkyrie")) { prof.kind="hammer"; prof.sound="hammer_sound_effect"; }
    if(n.includes("runesage") || n.includes("mage")) { prof.kind="magic"; prof.sound="fireball_sound_effect"; }
    if(n.includes("kitsune")) { prof.kind="magic"; prof.sound="fireball_sound_effect"; }
    if(n.includes("corsair") || n.includes("buccaneer")) { prof.kind="gun"; prof.sound="gunshot_sound_effect"; }
    if(n.includes("beast tamer")) { prof.kind="arrow"; prof.sound="sound_arrow_effect"; }
    if(n.includes("rune dancer")) { prof.kind="sword"; prof.sound="sword_slash_sound_effect_no_copyright"; }
    if(n.includes("demon king") || n.includes("shadow rogue")) { prof.kind="dark"; prof.sound="evil_magic_spell_sound_design"; }
    if(n.includes("sun cleric") || n.includes("radiant cleric")) { prof.kind="water"; prof.sound="piranha_attack_sound_effect_water"; }

    return prof;
  };

  // Boss attack extras: rune bolts + hurt sound + halo hue
  const _origDoBossAttackFX = window.doBossAttackFX_;
  if(typeof _origDoBossAttackFX === "function"){
    window.doBossAttackFX_ = function(){
      const r = window.app && window.app.rpg;
      const boss = r && r.boss;
      const kind = kindFromBoss_(boss);
      ensureRuneHalo_();
      if(boss && typeof boss.hue === "number") setBossHaloHue_(boss.hue);
      const ret = _origDoBossAttackFX.apply(this, arguments);

      // Boss SFX reinforce (on damage)
      safeTry(()=>{ const prof = (typeof window.bossAttackProfile_==="function" ? window.bossAttackProfile_(boss) : null); if(prof && prof.sound && typeof window.playAssetSound_==="function"){ window.playAssetSound_(prof.sound, 0.55); } });

      // Extra rune fire circles -> hero
      const bossEl = byId("bossSprite");
      const heroEl = byId("heroSprite");
      for(let i=0;i<3;i++){
        setTimeout(()=>spawnRuneBolt_(bossEl, heroEl, kind), i*90);
      }
      // Make the hit feel impactful
      setTimeout(()=>{ heroHurtSfx_(); hitShake_(heroEl); }, 120);

      return ret;
    };
  }

  // Keep halos alive even if UI rerenders
  document.addEventListener("DOMContentLoaded", ()=>{
    ensureRuneHalo_();
    // occasionally re-ensure for dynamic renders
    setInterval(ensureRuneHalo_, 1200);
  });

})();
