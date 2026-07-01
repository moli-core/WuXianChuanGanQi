/**
 * 数字孪生 3D 可视化引擎 v2.0
 * ──────────────────────────────────────────
 * 6区模块化架构：
 *   1. 场景初始化    Scene / Camera / Renderers / Lights
 *   2. 户型构建      地板分区 / 天花板 / 墙体 / 门窗 / 家具
 *   3. IoT设备实体   人体感应 / 蜂鸣器 / 三色灯 / 温湿度传感器
 *   4. 状态管理器    字段映射 / setDeviceState / setTempHumi / 联动规则
 *   5. 动画系统      平滑过渡 / 报警波纹 / 脉冲动画 / 光晕
 *   6. API暴露+循环  window接口 / 渲染循环 / 自适应
 *
 * 对接华为云IoTDA物模型：service_id="Esp32"
 * 硬件字段: Status_LED | Status_ledRed | Status_ledYellow | Status_beeper | Status_body | Data_temp | Data_humi
 */

import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { CSS2DRenderer, CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js';

// ╔══════════════════════════════════════════════════════════════╗
// ║  第0区：程序化纹理生成  Canvas API → PBR贴图                  ║
// ╚══════════════════════════════════════════════════════════════╝

function createCanvasTexture(w, h, drawFn) {
    const canvas = document.createElement('canvas');
    canvas.width = w;
    canvas.height = h;
    const ctx = canvas.getContext('2d');
    drawFn(ctx, w, h);
    const tex = new THREE.CanvasTexture(canvas);
    tex.wrapS = THREE.RepeatWrapping;
    tex.wrapT = THREE.RepeatWrapping;
    tex.colorSpace = THREE.SRGBColorSpace;
    tex.magFilter = THREE.LinearFilter;
    tex.minFilter = THREE.LinearMipmapLinearFilter;
    tex.generateMipmaps = true;
    return tex;
}

// 木地板纹理 (浅橡木 — 现代简约)
function generateWoodFloorTexture() {
    return createCanvasTexture(512, 512, (ctx, w, h) => {
        ctx.fillStyle = '#d9c8a8';
        ctx.fillRect(0, 0, w, h);
        // 细腻木纹
        for (let y = 0; y < h; y++) {
            const wave = Math.sin(y * 0.04 + Math.sin(y * 0.012) * 2.5) * 0.5 + 0.5;
            ctx.fillStyle = `rgba(180,155,120,${wave * 0.08})`;
            ctx.fillRect(0, y, w, 1);
        }
        // 长条地板接缝
        const plankW = w / 6;
        for (let i = 0; i < 6; i++) {
            const x = i * plankW;
            const grad = ctx.createLinearGradient(x - 1.5, 0, x + 1.5, 0);
            grad.addColorStop(0, 'rgba(0,0,0,0)');
            grad.addColorStop(0.5, 'rgba(0,0,0,0.15)');
            grad.addColorStop(1, 'rgba(0,0,0,0)');
            ctx.fillStyle = grad;
            ctx.fillRect(x - 1.5, 0, 3, h);
        }
        // 极细噪点
        for (let i = 0; i < 1500; i++) {
            ctx.fillStyle = `rgba(0,0,0,${Math.random() * 0.015})`;
            ctx.fillRect(Math.random() * w, Math.random() * h, 1, 1);
        }
    });
}

// 瓷砖纹理 (浅灰 — 现代简约卫生间)
function generateTileTexture() {
    return createCanvasTexture(256, 256, (ctx, w, h) => {
        ctx.fillStyle = '#e8e5e0';
        ctx.fillRect(0, 0, w, h);
        const grid = 2; // 大块瓷砖
        const cell = w / grid;
        for (let r = 0; r < grid; r++) {
            for (let c = 0; c < grid; c++) {
                const shade = 0.95 + Math.random() * 0.05;
                const val = Math.floor(232 * shade);
                ctx.fillStyle = `rgb(${val},${val - 2},${val - 5})`;
                ctx.fillRect(c * cell + 1, r * cell + 1, cell - 2, cell - 2);
            }
        }
        // 极细勾缝
        ctx.fillStyle = 'rgba(200,198,194,0.5)';
        ctx.fillRect(cell - 1, 0, 2, h);
        ctx.fillRect(0, cell - 1, w, 2);
    });
}

// 墙壁纹理 (极简光滑白)
function generateWallTexture(baseColor) {
    return createCanvasTexture(256, 256, (ctx, w, h) => {
        ctx.fillStyle = baseColor;
        ctx.fillRect(0, 0, w, h);
        // 非常微弱的噪点
        for (let i = 0; i < 2000; i++) {
            ctx.fillStyle = `rgba(0,0,0,${Math.random() * 0.008})`;
            ctx.fillRect(Math.random() * w, Math.random() * h, 1, 1);
        }
    });
}

// 织物纹理 (纯色极简)
function generateFabricTexture(baseColor) {
    return createCanvasTexture(128, 128, (ctx, w, h) => {
        ctx.fillStyle = baseColor;
        ctx.fillRect(0, 0, w, h);
        for (let i = 0; i < 600; i++) {
            ctx.fillStyle = `rgba(0,0,0,${Math.random() * 0.02})`;
            ctx.fillRect(Math.random() * w, Math.random() * h, 1, 1);
        }
    });
}

// 木纹纹理 (浅橡木 — 家具用)
function generateWoodGrainTexture(baseColor) {
    return createCanvasTexture(256, 256, (ctx, w, h) => {
        ctx.fillStyle = baseColor;
        ctx.fillRect(0, 0, w, h);
        for (let y = 0; y < h; y++) {
            const wave = Math.sin(y * 0.025 + Math.sin(y * 0.01) * 3) * 0.5 + 0.5;
            ctx.fillStyle = `rgba(140,110,70,${wave * 0.1})`;
            ctx.fillRect(0, y, w, 1);
        }
    });
}

// 地毯纹理 (简约几何图案)
function generateRugTexture(baseColor, accentColor) {
    return createCanvasTexture(256, 256, (ctx, w, h) => {
        ctx.fillStyle = baseColor;
        ctx.fillRect(0, 0, w, h);
        // 简约边框
        ctx.strokeStyle = accentColor;
        ctx.lineWidth = 2;
        ctx.globalAlpha = 0.3;
        ctx.strokeRect(16, 16, w - 32, h - 32);
        ctx.globalAlpha = 1;
        // 极细编织纹理
        for (let i = 0; i < 1500; i++) {
            ctx.fillStyle = `rgba(0,0,0,${Math.random() * 0.02})`;
            ctx.fillRect(Math.random() * w, Math.random() * h, 1, 1);
        }
    });
}

// 大理石纹理 (卫生间台面)
function generateMarbleTexture() {
    return createCanvasTexture(256, 256, (ctx, w, h) => {
        ctx.fillStyle = '#f6f4f0';
        ctx.fillRect(0, 0, w, h);
        for (let i = 0; i < 8; i++) {
            ctx.beginPath();
            const sx = Math.random() * w;
            const sy = Math.random() * h;
            ctx.moveTo(sx, sy);
            for (let j = 0; j < 12; j++) {
                ctx.lineTo(
                    sx + (j - 6) * 20 + Math.sin(j * 0.4) * 30,
                    sy + Math.cos(j * 0.25) * 40
                );
            }
            ctx.strokeStyle = `rgba(180,175,168,${0.04 + Math.random() * 0.03})`;
            ctx.lineWidth = 0.8 + Math.random() * 1.2;
            ctx.stroke();
        }
    });
}

// 预生成纹理（现代简约版）
const Tex = {
    woodFloor: generateWoodFloorTexture(),
    tileFloor: generateTileTexture(),
    wallWhite: generateWallTexture('#f7f5f2'),
    wallGrey: generateWallTexture('#ece9e5'),
    fabricGrey: generateFabricTexture('#b0aca8'),
    fabricBeige: generateFabricTexture('#c8c0b4'),
    fabricCharcoal: generateFabricTexture('#5a5552'),
    woodOak: generateWoodGrainTexture('#d4c0a0'),
    woodDark: generateWoodGrainTexture('#8a7560'),
    woodLight: generateWoodGrainTexture('#e0d0b8'),
    rugLiving: generateRugTexture('#e8e2d8', '#c8c0b0'),
    rugBedroom: generateRugTexture('#e5dfd5', '#c0b8a8'),
    marble: generateMarbleTexture(),
};

// 设置纹理重复
Tex.woodFloor.repeat.set(2, 2);
Tex.tileFloor.repeat.set(2, 2);
Tex.wallWhite.repeat.set(2, 1);
Tex.wallGrey.repeat.set(2, 1);
Tex.fabricGrey.repeat.set(2, 2);
Tex.fabricBeige.repeat.set(2, 2);
Tex.fabricCharcoal.repeat.set(2, 2);
Tex.woodOak.repeat.set(1.5, 1.5);
Tex.woodDark.repeat.set(1.5, 1.5);
Tex.woodLight.repeat.set(1.5, 1.5);
Tex.marble.repeat.set(1, 1);

// ╔══════════════════════════════════════════════════════════════╗
// ║  第1区：场景初始化  Scene / Camera / Renderers / Lights      ║
// ╚══════════════════════════════════════════════════════════════╝

const scene = new THREE.Scene();
scene.background = new THREE.Color(0xd8dce4);
scene.fog = new THREE.Fog(0xd8dce4, 20, 70);

const camera = new THREE.PerspectiveCamera(40, window.innerWidth / window.innerHeight, 0.1, 1000);
camera.position.set(14, 10, 17);
camera.lookAt(0, 1, 0);

// WebGL渲染器
const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 1.55;
document.body.appendChild(renderer.domElement);

// CSS2D标签渲染器
const labelRenderer = new CSS2DRenderer();
labelRenderer.setSize(window.innerWidth, window.innerHeight);
labelRenderer.domElement.style.position = 'absolute';
labelRenderer.domElement.style.top = '0px';
labelRenderer.domElement.style.left = '0px';
labelRenderer.domElement.style.pointerEvents = 'none';
document.body.appendChild(labelRenderer.domElement);

// 轨道控制器
const controls = new OrbitControls(camera, renderer.domElement);
controls.enableDamping = true;
controls.dampingFactor = 0.08;
controls.maxPolarAngle = Math.PI / 2.2;
controls.target.set(0, 1.2, 0);
controls.update();

// 全局光照体系
const ambientLight = new THREE.AmbientLight(0x8898b0, 0.7);
scene.add(ambientLight);

const hemiLight = new THREE.HemisphereLight(0xfffff8, 0x444444, 0.4);
scene.add(hemiLight);

const sunLight = new THREE.DirectionalLight(0xfffaf2, 1.3);
sunLight.position.set(15, 22, 10);
sunLight.castShadow = true;
sunLight.shadow.mapSize.set(4096, 4096);
sunLight.shadow.camera.near = 0.5;
sunLight.shadow.camera.far = 80;
sunLight.shadow.camera.left = -18;
sunLight.shadow.camera.right = 18;
sunLight.shadow.camera.top = 18;
sunLight.shadow.camera.bottom = -18;
sunLight.shadow.bias = -0.00008;
sunLight.shadow.normalBias = 0.02;
scene.add(sunLight);

const fillLight = new THREE.DirectionalLight(0x8899cc, 0.3);
fillLight.position.set(-8, 5, -8);
scene.add(fillLight);

// 地板参考网格（淡化，仅辅助定位）
const gridHelper = new THREE.GridHelper(32, 32, 0x334466, 0x1a2430);
gridHelper.position.y = -0.01;
gridHelper.material.opacity = 0.15;
gridHelper.material.transparent = true;
scene.add(gridHelper);

// ╔══════════════════════════════════════════════════════════════╗
// ║  第2区：户型构建（十字走廊 + 四房分离）                       ║
// ╚══════════════════════════════════════════════════════════════╝
//
//  新布局：9.2m × 9.2m，十字走廊宽1.2m分隔四个房间
//  ┌───────────┬───────────┐
//  │  次卧 NW   │ 卫生间 NE  │  z=4.6(北)
//  │  4.0×4.0  │  4.0×4.0  │
//  ├──── ──────┼──── ──────┤  z=0.6
//  │   ║       │   ║       │
//  │ ══╬═══════╬══ ══     │  z=-0.6
//  │   ║ 走廊  │   ║       │
//  ├──── ──────┼──── ──────┤
//  │  客厅 SW   │  主卧 SE  │  z=-4.6(南)
//  │  4.0×4.0  │  4.0×4.0  │
//  └───▢入户───┴───────────┘
//    x=-4.6      x=4.6
//    (西)          (东)

const WALL_H = 2.8;
const WALL_THICK = 0.2;
const CORR = 0.6;  // 走廊半宽

// ----- 工具函数：创建实心墙体（带纹理）-----
function createWall(w, h, d, tex, x, y, z, roughness = 0.55) {
    const geo = new THREE.BoxGeometry(w, h, d);
    const mat = tex
        ? new THREE.MeshStandardMaterial({ map: tex, roughness, metalness: 0.05, color: 0xffffff })
        : new THREE.MeshStandardMaterial({ color: 0xe8e4e0, roughness, metalness: 0.05 });
    const wall = new THREE.Mesh(geo, mat);
    wall.position.set(x, y, z);
    wall.castShadow = true;
    wall.receiveShadow = true;
    scene.add(wall);
    return wall;
}

// ----- 地板 -----
const floorGeo = new THREE.BoxGeometry(9.2, 0.15, 9.2);
const floorMat = new THREE.MeshStandardMaterial({ map: Tex.woodFloor, roughness: 0.55, metalness: 0.05 });
const floor = new THREE.Mesh(floorGeo, floorMat);
floor.position.set(0, -0.075, 0);
floor.receiveShadow = true;
scene.add(floor);

// 房间地板分区
function addFloorZone(cx, cz, w, d, tex) {
    const geo = new THREE.PlaneGeometry(w, d);
    const mat = new THREE.MeshStandardMaterial({
        map: tex, roughness: 0.5, metalness: 0.05, transparent: true, opacity: 0.85, side: THREE.DoubleSide
    });
    const zone = new THREE.Mesh(geo, mat);
    zone.rotation.x = -Math.PI / 2;
    zone.position.set(cx, 0.005, cz);
    zone.receiveShadow = true;
    scene.add(zone);
}
// 四个房间地板分区（4.0×4.0m，走廊不含在内）
addFloorZone(-2.6, -2.6, 4.0, 4.0, Tex.woodFloor);   // 客厅 SW
addFloorZone( 2.6, -2.6, 4.0, 4.0, Tex.woodFloor);   // 主卧 SE
addFloorZone(-2.6,  2.6, 4.0, 4.0, Tex.woodFloor);   // 次卧 NW
addFloorZone( 2.6,  2.6, 4.0, 4.0, Tex.tileFloor);   // 卫生间 NE
// 走廊地板分区
addFloorZone(0, -2.6, 1.2, 4.0, Tex.woodFloor);       // 南走廊
addFloorZone(0,  2.6, 1.2, 4.0, Tex.woodFloor);       // 北走廊
addFloorZone(-2.6, 0, 4.0, 1.2, Tex.woodFloor);       // 西走廊
addFloorZone( 2.6, 0, 4.0, 1.2, Tex.woodFloor);       // 东走廊

// ----- 天花板：已移除 -----

// ----- 外墙 -----
const extWallTex = Tex.wallWhite;
// 南墙（两段，中间入户门 gap）
createWall(4.0, WALL_H, WALL_THICK, extWallTex, -2.6, WALL_H/2, -4.6, 0.6);
createWall(4.0, WALL_H, WALL_THICK, extWallTex,  2.6, WALL_H/2, -4.6, 0.6);
// 北墙（完整）
createWall(9.4, WALL_H, WALL_THICK, extWallTex, 0, WALL_H/2, 4.6, 0.6);
// 西墙（完整）
createWall(WALL_THICK, WALL_H, 9.4, extWallTex, -4.6, WALL_H/2, 0, 0.6);
// 东墙（完整）
createWall(WALL_THICK, WALL_H, 9.4, extWallTex, 4.6, WALL_H/2, 0, 0.6);

// ----- 内墙：十字走廊分隔 -----
// 走廊边界：x=±0.6, z=±0.6
// 每面内墙由两段组成（留出门洞）

function createInteriorWallPair(w1, cz1, w2, cz2, cx, isX) {
    // isX=true: 墙沿X轴，在z=cz处（w=宽度沿X, d=厚度）
    // isX=false: 墙沿Z轴，在x=cx处（w=厚度, d=深度沿Z）
    const intTex = Tex.wallWhite;
    if (isX) {
        if (w1 > 0.01) createWall(w1, WALL_H, WALL_THICK, intTex, cx, WALL_H/2, cz1, 0.55);
        if (w2 > 0.01) createWall(w2, WALL_H, WALL_THICK, intTex, cx, WALL_H/2, cz2, 0.55);
    } else {
        if (w1 > 0.01) createWall(WALL_THICK, WALL_H, w1, intTex, cx, WALL_H/2, cz1, 0.55);
        if (w2 > 0.01) createWall(WALL_THICK, WALL_H, w2, intTex, cx, WALL_H/2, cz2, 0.55);
    }
}

// 南走廊墙 z=-0.6（分隔客厅/主卧与走廊）— 无门洞，完整墙
createWall(4.0, WALL_H, WALL_THICK, Tex.wallWhite, -2.6, WALL_H/2, -CORR, 0.55);
createWall(4.0, WALL_H, WALL_THICK, Tex.wallWhite,  2.6, WALL_H/2, -CORR, 0.55);

// 北走廊墙 z=0.6（分隔次卧/卫生间与走廊）— 无门洞，完整墙
createWall(4.0, WALL_H, WALL_THICK, Tex.wallWhite, -2.6, WALL_H/2, CORR, 0.55);
createWall(4.0, WALL_H, WALL_THICK, Tex.wallWhite,  2.6, WALL_H/2, CORR, 0.55);

// 西走廊墙 x=-0.6（分隔客厅/次卧与走廊）— 有门洞
// 客厅侧：z=[-4.6, -2.6] + [−1.4, -0.6]，门洞 z=[-2.6, -1.4]
createInteriorWallPair(2.0, -3.6, 0.8, -1.0, -CORR, false);
// 次卧侧：z=[0.6, 1.4] + [2.6, 4.6]，门洞 z=[1.4, 2.6]
createInteriorWallPair(0.8, 1.0, 2.0, 3.6, -CORR, false);

// 东走廊墙 x=0.6（分隔主卧/卫生间与走廊）— 有门洞
createInteriorWallPair(2.0, -3.6, 0.8, -1.0, CORR, false);
createInteriorWallPair(0.8, 1.0, 2.0, 3.6, CORR, false);

// ----- 踢脚线 -----
function addBaseboard(w, x, y, z, rotY = 0) {
    const geo = new THREE.BoxGeometry(w, 0.1, 0.04);
    const mat = new THREE.MeshStandardMaterial({ map: Tex.woodDark, roughness: 0.4, metalness: 0.05 });
    const board = new THREE.Mesh(geo, mat);
    board.position.set(x, y, z);
    board.rotation.y = rotY;
    board.castShadow = true;
    board.receiveShadow = true;
    scene.add(board);
}
// 外墙踢脚线
addBaseboard(4.0, -2.6, 0.05, -4.5);          // 南墙西段
addBaseboard(4.0,  2.6, 0.05, -4.5);          // 南墙东段
addBaseboard(9.4, 0,    0.05,  4.5);          // 北墙
addBaseboard(9.4, -4.5, 0.05, 0, Math.PI/2);  // 西墙
addBaseboard(9.4,  4.5, 0.05, 0, Math.PI/2);  // 东墙

// ----- 窗户系统 -----
function addWindow(wx, wy, wz, ww, wh, rotY = 0) {
    const winGroup = new THREE.Group();
    const frameOuter = new THREE.Mesh(
        new THREE.BoxGeometry(ww + 0.1, wh + 0.1, 0.06),
        new THREE.MeshStandardMaterial({ color: 0xf8f4f0, roughness: 0.3, metalness: 0.1 })
    );
    frameOuter.castShadow = true;
    const glassGeo = new THREE.PlaneGeometry(ww - 0.05, wh - 0.05);
    const glassMat = new THREE.MeshPhysicalMaterial({
        color: 0xddeeff, roughness: 0.05, metalness: 0.05,
        transparent: true, opacity: 0.25, envMapIntensity: 0.5, clearcoat: 0.1
    });
    const glass = new THREE.Mesh(glassGeo, glassMat);
    glass.position.z = 0.01;
    const mullionH = new THREE.Mesh(
        new THREE.BoxGeometry(ww + 0.05, 0.04, 0.04),
        new THREE.MeshStandardMaterial({ color: 0xf8f4f0, roughness: 0.3 })
    );
    mullionH.position.z = 0.02;
    const mullionV = new THREE.Mesh(
        new THREE.BoxGeometry(0.04, wh + 0.05, 0.04),
        new THREE.MeshStandardMaterial({ color: 0xf8f4f0, roughness: 0.3 })
    );
    mullionV.position.z = 0.02;
    winGroup.add(frameOuter, glass, mullionH, mullionV);
    winGroup.position.set(wx, wy, wz);
    winGroup.rotation.y = rotY;
    winGroup.castShadow = true;
    scene.add(winGroup);
    return winGroup;
}
// 客厅窗户（南墙）
addWindow(-2.6, 1.8, -4.6, 1.4, 1.3, 0);
// 主卧窗户（南墙）
addWindow( 2.6, 1.8, -4.6, 1.4, 1.3, 0);
// 次卧窗户（西墙）
addWindow(-4.6, 1.8,  2.6, 1.2, 1.2, Math.PI/2);
// 卫生间窗户（东墙）
addWindow( 4.6, 1.8,  3.2, 0.9, 0.9, -Math.PI/2);

// ----- 窗帘 -----
function addCurtain(x, y, z, w, h, rotY = 0) {
    const group = new THREE.Group();
    const curtainMat = new THREE.MeshStandardMaterial({
        map: Tex.fabricBeige, roughness: 0.85, metalness: 0,
        transparent: true, opacity: 0.75, side: THREE.DoubleSide
    });
    const leftCurtain = new THREE.Mesh(new THREE.PlaneGeometry(w/2+0.08, h+0.15), curtainMat);
    leftCurtain.position.set(-w/4, 0, 0.08);
    group.add(leftCurtain);
    const rightCurtain = new THREE.Mesh(new THREE.PlaneGeometry(w/2+0.08, h+0.15), curtainMat);
    rightCurtain.position.set(w/4, 0, 0.08);
    group.add(rightCurtain);
    const rodGeo = new THREE.CylinderGeometry(0.03, 0.03, w+0.2, 8);
    const rodMat = new THREE.MeshStandardMaterial({ color: 0x3a2a1a, roughness: 0.5, metalness: 0.3 });
    const rod = new THREE.Mesh(rodGeo, rodMat);
    rod.rotation.z = Math.PI/2;
    rod.position.set(0, h/2+0.1, 0.05);
    rod.castShadow = true;
    group.add(rod);
    group.position.set(x, y, z);
    group.rotation.y = rotY;
    scene.add(group);
}
addCurtain(-2.6, 1.8, -4.55, 1.5, 1.4, 0);
addCurtain( 2.6, 1.8, -4.55, 1.5, 1.4, 0);

// ----- 装饰画 -----
function addWallArt(x, y, z, w, h, rotY = 0) {
    const group = new THREE.Group();
    const frameGeo = new THREE.BoxGeometry(w+0.12, h+0.12, 0.03);
    const frame = new THREE.Mesh(frameGeo, new THREE.MeshStandardMaterial({ color: 0x3a2a1a, roughness: 0.4, metalness: 0.1 }));
    frame.castShadow = true;
    group.add(frame);
    const canvasGeo = new THREE.PlaneGeometry(w, h);
    const canvas = new THREE.Mesh(canvasGeo, new THREE.MeshStandardMaterial({
        color: new THREE.Color(`hsl(${Math.random()*360}, 25%, 70%)`), roughness: 0.7, metalness: 0
    }));
    canvas.position.z = 0.02;
    group.add(canvas);
    group.position.set(x, y, z);
    group.rotation.y = rotY;
    scene.add(group);
}
addWallArt(-2.6, 1.8, -4.55, 1.0, 0.7, 0);        // 客厅
addWallArt( 2.6, 1.8, -4.55, 0.7, 0.6, 0);        // 主卧
addWallArt(-4.55, 1.6, 2.6, 0.6, 0.5, Math.PI/2);  // 次卧

// ----- 门框系统（可旋转，适配不同朝向墙体）-----
function addDoorFrame(dx, dz, isGate = false, rotY = 0) {
    const group = new THREE.Group();
    const pillarMat = new THREE.MeshStandardMaterial({ map: Tex.woodDark, roughness: 0.5, metalness: 0.05 });
    const pillarGeo = new THREE.BoxGeometry(0.15, 2.0, 0.15);
    const beamGeo = new THREE.BoxGeometry(1.2, 0.18, 0.15);

    const p1 = new THREE.Mesh(pillarGeo, pillarMat);
    p1.position.set(-0.6, 1.0, 0);
    p1.castShadow = true;
    const p2 = new THREE.Mesh(pillarGeo, pillarMat);
    p2.position.set( 0.6, 1.0, 0);
    p2.castShadow = true;
    const beam = new THREE.Mesh(beamGeo, pillarMat);
    beam.position.set(0, 2.0, 0);
    beam.castShadow = true;
    group.add(p1, p2, beam);

    if (isGate) {
        const doorMat = new THREE.MeshStandardMaterial({ map: Tex.woodDark, roughness: 0.45, metalness: 0.05 });
        const door = new THREE.Mesh(new THREE.BoxGeometry(1.1, 2.0, 0.08), doorMat);
        door.position.set(0, 1.0, -0.04);
        door.castShadow = true;
        door.receiveShadow = true;
        group.add(door);
        const knobGeo = new THREE.SphereGeometry(0.04, 8, 8);
        const knob = new THREE.Mesh(knobGeo, new THREE.MeshStandardMaterial({ color: 0xd0c8b8, roughness: 0.2, metalness: 0.9 }));
        knob.position.set(0.35, 1.05, 0.02);
        group.add(knob);
    }

    group.position.set(dx, 0, dz);
    group.rotation.y = rotY;
    scene.add(group);
}

// 入户大门（南墙，开口沿X）
addDoorFrame(0, -4.6, true, 0);
// 客厅门（西走廊墙 x=-0.6，开口沿Z）→ rotY=π/2
addDoorFrame(-CORR, -2.0, false, Math.PI/2);
// 主卧门（东走廊墙 x=0.6，开口沿Z）
addDoorFrame( CORR, -2.0, false, Math.PI/2);
// 次卧门（西走廊墙 x=-0.6，开口沿Z）
addDoorFrame(-CORR,  2.0, false, Math.PI/2);
// 卫生间门（东走廊墙 x=0.6，开口沿Z）
addDoorFrame( CORR,  2.0, false, Math.PI/2);

// ----- 房间标签 -----
function createLabel(text, posX, posY, posZ, accentColor = '#88ddff') {
    const div = document.createElement('div');
    div.textContent = text;
    div.style.cssText = `
        color:#fff; font-size:17px; font-weight:bold;
        text-shadow:2px 2px 8px rgba(0,0,0,0.9);
        background:rgba(15,22,35,0.7); padding:4px 14px;
        border-radius:20px; border:1px solid ${accentColor};
        backdrop-filter:blur(4px); pointer-events:none;
        white-space:nowrap;
    `;
    const label = new CSS2DObject(div);
    label.position.set(posX, posY, posZ);
    return label;
}
scene.add(createLabel('🛋️ 客厅',  -2.6, 1.5, -3.5, '#ffaa66'));
scene.add(createLabel('🛏️ 主卧',   2.6, 1.5, -3.5, '#66aaff'));
scene.add(createLabel('🛏️ 次卧',  -2.6, 1.5,  3.5, '#66aaff'));
scene.add(createLabel('🚿 卫生间', 2.6, 1.5,  3.5, '#66ffaa'));
scene.add(createLabel('🚪 入户PIR感应', 0, 2.0, -4.8, '#ff4444'));
scene.add(createLabel('🌡️ 温湿度', -2.6, 2.5, -0.8, '#22bb88'));

// ╔══════════════════════════════════════════════════════════════╗
// ║  第2.5区：家具与室内美化                                    ║
// ╚══════════════════════════════════════════════════════════════╝

// ----- 材质库（现代简约版）-----
const Mat = {
    wood:      new THREE.MeshStandardMaterial({ map: Tex.woodOak, roughness: 0.45, metalness: 0.03 }),
    woodDark:  new THREE.MeshStandardMaterial({ map: Tex.woodDark, roughness: 0.4, metalness: 0.03 }),
    woodLight: new THREE.MeshStandardMaterial({ map: Tex.woodLight, roughness: 0.45, metalness: 0.03 }),
    fabric:    new THREE.MeshStandardMaterial({ map: Tex.fabricGrey, roughness: 0.85, metalness: 0 }),
    fabricWarm:new THREE.MeshStandardMaterial({ map: Tex.fabricBeige, roughness: 0.85, metalness: 0 }),
    fabricDark:new THREE.MeshStandardMaterial({ map: Tex.fabricCharcoal, roughness: 0.85, metalness: 0 }),
    leather:   new THREE.MeshStandardMaterial({ color: 0x3d3530, roughness: 0.4, metalness: 0.03 }),
    white:     new THREE.MeshStandardMaterial({ color: 0xf8f6f2, roughness: 0.12, metalness: 0.03 }),
    metal:     new THREE.MeshStandardMaterial({ color: 0x3a3a3a, roughness: 0.2, metalness: 0.95 }),
    black:     new THREE.MeshStandardMaterial({ color: 0x1a1a1a, roughness: 0.25, metalness: 0.08 }),
    glass:     new THREE.MeshPhysicalMaterial({ color: 0xe8f0f4, roughness: 0.03, metalness: 0.02, transparent: true, opacity: 0.35, envMapIntensity: 0.3 }),
    carpet:    new THREE.MeshStandardMaterial({ map: Tex.rugLiving, roughness: 0.95, metalness: 0 }),
    carpetBed: new THREE.MeshStandardMaterial({ map: Tex.rugBedroom, roughness: 0.95, metalness: 0 }),
    tile:      new THREE.MeshStandardMaterial({ map: Tex.tileFloor, roughness: 0.25, metalness: 0.03 }),
    mattress:  new THREE.MeshStandardMaterial({ color: 0xfafaf8, roughness: 0.75, metalness: 0 }),
    sheet:     new THREE.MeshStandardMaterial({ map: Tex.fabricGrey, roughness: 0.75, metalness: 0 }),
    marble:    new THREE.MeshStandardMaterial({ map: Tex.marble, roughness: 0.2, metalness: 0.05 }),
};

// ----- 辅助：创建带圆角的Box（用多个box近似）-----
function addBox(parent, w, h, d, material, x, y, z) {
    const m = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), material);
    m.position.set(x, y, z);
    m.castShadow = true;
    m.receiveShadow = true;
    parent.add(m);
    return m;
}

// ==================== 客厅家具（增强版）====================
function buildLivingRoom() {
    const room = new THREE.Group();

    // -- L型转角沙发（现代简约：深灰面料 + 浅木框架）--
    const sofa = new THREE.Group();
    // 底座框架（浅橡木）
    addBox(sofa, 2.5, 0.12, 0.95, Mat.woodLight, 0, 0.06, 0);
    // 主座垫 x3（深灰织物）
    for (let i = -1; i <= 1; i++) {
        const cushionGeo = new THREE.BoxGeometry(0.72, 0.16, 0.8, 3, 3, 3);
        const cushion = new THREE.Mesh(cushionGeo, Mat.fabricDark);
        cushion.position.set(i * 0.78, 0.23, 0);
        cushion.castShadow = true;
        cushion.receiveShadow = true;
        sofa.add(cushion);
    }
    // 靠背
    const backGeo = new THREE.BoxGeometry(2.5, 0.55, 0.18, 4, 3, 2);
    const back = new THREE.Mesh(backGeo, Mat.fabricDark);
    back.position.set(0, 0.58, -0.42);
    back.castShadow = true;
    back.receiveShadow = true;
    sofa.add(back);
    // 扶手（浅木）
    const armGeo = new THREE.BoxGeometry(0.14, 0.45, 0.95, 2, 3, 2);
    for (const ax of [-1.28, 1.28]) {
        const arm = new THREE.Mesh(armGeo, Mat.woodLight);
        arm.position.set(ax, 0.33, 0);
        arm.castShadow = true;
        arm.receiveShadow = true;
        sofa.add(arm);
    }
    // 转角延伸
    addBox(sofa, 1.15, 0.16, 0.85, Mat.fabricDark, 1.35, 0.23, 0.85);
    const extBackGeo = new THREE.BoxGeometry(1.15, 0.55, 0.18, 4, 3, 2);
    const extBack = new THREE.Mesh(extBackGeo, Mat.fabricDark);
    extBack.position.set(1.35, 0.58, 1.32);
    extBack.castShadow = true;
    sofa.add(extBack);
    // 靠垫 x2（浅灰）
    for (let i = -1; i <= 1; i += 2) {
        const pillowGeo = new THREE.BoxGeometry(0.48, 0.25, 0.08, 4, 4, 2);
        const pillow = new THREE.Mesh(pillowGeo, Mat.fabricWarm);
        pillow.position.set(i * 0.65, 0.42, -0.3);
        pillow.rotation.x = -0.12;
        pillow.castShadow = true;
        sofa.add(pillow);
    }
    sofa.position.set(-3.3, 0, -1.8);
    room.add(sofa);

    // -- 茶几（木质台面 + 金属腿）--
    const table = new THREE.Group();
    addBox(table, 1.35, 0.06, 0.75, Mat.woodLight, 0, 0.52, 0);
    // 金属腿
    const legGeo = new THREE.CylinderGeometry(0.03, 0.03, 0.44, 8);
    for (const [lx, lz] of [[-0.52, -0.3], [0.52, -0.3], [-0.52, 0.3], [0.52, 0.3]]) {
        const leg = new THREE.Mesh(legGeo, Mat.metal);
        leg.position.set(lx, 0.22, lz);
        leg.castShadow = true;
        table.add(leg);
    }
    addBox(table, 1.05, 0.03, 0.5, Mat.woodLight, 0, 0.16, 0);
    const bookGeo = new THREE.BoxGeometry(0.18, 0.03, 0.25, 2, 1, 2);
    const book1 = new THREE.Mesh(bookGeo, new THREE.MeshStandardMaterial({ color: 0xd8443a, roughness: 0.5 }));
    book1.position.set(0.2, 0.56, 0.1);
    book1.rotation.y = 0.2;
    book1.castShadow = true;
    table.add(book1);
    const book2 = new THREE.Mesh(bookGeo, new THREE.MeshStandardMaterial({ color: 0x3366aa, roughness: 0.5 }));
    book2.position.set(0.25, 0.59, 0.07);
    book2.rotation.y = 0.35;
    book2.castShadow = true;
    table.add(book2);
    table.position.set(-3.0, 0, -2.8);
    room.add(table);

    // -- 电视柜 --
    const tvStand = new THREE.Group();
    addBox(tvStand, 2.3, 0.45, 0.52, Mat.wood, 0, 0.22, 0);
    addBox(tvStand, 2.3, 0.04, 0.52, Mat.woodDark, 0, 0.45, 0);
    addBox(tvStand, 0.02, 0.42, 0.46, Mat.woodDark, 0, 0.22, 0);
    const screenGeo = new THREE.BoxGeometry(1.65, 0.95, 0.06, 2, 2, 1);
    const screenMat = new THREE.MeshStandardMaterial({ color: 0x111118, roughness: 0.15, metalness: 0.2 });
    const screen = new THREE.Mesh(screenGeo, screenMat);
    screen.position.set(0, 0.95, -0.24);
    screen.castShadow = true;
    tvStand.add(screen);
    const bezelGeo = new THREE.BoxGeometry(1.75, 1.05, 0.03);
    const bezel = new THREE.Mesh(bezelGeo, Mat.black);
    bezel.position.set(0, 0.95, -0.29);
    tvStand.add(bezel);
    const baseGeo = new THREE.BoxGeometry(0.6, 0.06, 0.25, 3, 2, 2);
    const base = new THREE.Mesh(baseGeo, Mat.metal);
    base.position.set(0, 0.48, -0.28);
    base.castShadow = true;
    tvStand.add(base);
    tvStand.position.set(-3.0, 0, -4.35);
    room.add(tvStand);

    // -- 地毯 --
    const rug = new THREE.Mesh(
        new THREE.PlaneGeometry(2.2, 1.8),
        Mat.carpet
    );
    rug.rotation.x = -Math.PI / 2;
    rug.position.set(-3.0, 0.008, -2.6);
    rug.receiveShadow = true;
    room.add(rug);

    // -- 落地灯 --
    const lamp = new THREE.Group();
    const poleGeo = new THREE.CylinderGeometry(0.04, 0.05, 1.65, 12);
    const pole = new THREE.Mesh(poleGeo, Mat.metal);
    pole.position.set(0, 0.82, 0);
    pole.castShadow = true;
    lamp.add(pole);
    const shadeGeo = new THREE.CylinderGeometry(0.14, 0.3, 0.5, 24);
    const shadeMat = new THREE.MeshStandardMaterial({
        color: 0xfff6e8, roughness: 0.5, emissive: 0x221100, emissiveIntensity: 0.1
    });
    const shade = new THREE.Mesh(shadeGeo, shadeMat);
    shade.position.set(0, 1.72, 0);
    shade.castShadow = true;
    lamp.add(shade);
    const basePlate = new THREE.Mesh(
        new THREE.CylinderGeometry(0.16, 0.18, 0.05, 16),
        Mat.metal
    );
    basePlate.position.set(0, 0.03, 0);
    basePlate.castShadow = true;
    lamp.add(basePlate);
    lamp.position.set(-4.3, 0, -1.5);
    room.add(lamp);

    // -- 绿植盆栽 --
    const plant = new THREE.Group();
    const potGeo = new THREE.CylinderGeometry(0.18, 0.14, 0.4, 16);
    const potMat = new THREE.MeshStandardMaterial({ color: 0xd4956a, roughness: 0.5 });
    const pot = new THREE.Mesh(potGeo, potMat);
    pot.position.set(0, 0.2, 0);
    pot.castShadow = true;
    plant.add(pot);
    for (let i = 0; i < 5; i++) {
        const leafGeo = new THREE.SphereGeometry(0.18 + i * 0.06, 8, 6);
        const leafMat = new THREE.MeshStandardMaterial({
            color: new THREE.Color().setHSL(0.22 + Math.random() * 0.08, 0.6 + Math.random() * 0.3, 0.28 + Math.random() * 0.2),
            roughness: 0.7
        });
        const leafBall = new THREE.Mesh(leafGeo, leafMat);
        leafBall.position.set(
            (Math.random() - 0.5) * 0.2,
            0.45 + i * 0.1,
            (Math.random() - 0.5) * 0.2
        );
        leafBall.scale.set(1, 0.7 + Math.random() * 0.2, 1);
        leafBall.castShadow = true;
        plant.add(leafBall);
    }
    plant.position.set(-4.3, 0, -0.9);
    room.add(plant);

    room.position.set(0, 0, 0);
    return room;
}

// ==================== 主卧家具（增强版）====================
function buildMasterBedroom() {
    const room = new THREE.Group();

    // -- 双人床 --
    const bed = new THREE.Group();
    // 床架底座
    addBox(bed, 2.1, 0.3, 2.4, Mat.wood, 0, 0.15, 0);
    // 床架边框（略突出）
    addBox(bed, 2.15, 0.08, 2.45, Mat.woodDark, 0, 0.32, 0);
    // 床头板（弧形造型 — 用大圆角Box近似）
    const headboardGeo = new THREE.BoxGeometry(2.15, 1.0, 0.12, 4, 6, 2);
    const headboard = new THREE.Mesh(headboardGeo, Mat.woodDark);
    headboard.position.set(0, 0.7, -1.15);
    headboard.castShadow = true;
    headboard.receiveShadow = true;
    bed.add(headboard);
    // 床垫（加厚 + 细分）
    const mattressGeo = new THREE.BoxGeometry(1.9, 0.2, 2.15, 6, 4, 6);
    const mattress = new THREE.Mesh(mattressGeo, Mat.mattress);
    mattress.position.set(0, 0.42, 0.05);
    mattress.castShadow = true;
    mattress.receiveShadow = true;
    bed.add(mattress);
    // 被子（折叠效果：双层）
    const duvetGeo = new THREE.BoxGeometry(1.95, 0.1, 1.7, 8, 3, 8);
    const duvet = new THREE.Mesh(duvetGeo, Mat.sheet);
    duvet.position.set(0, 0.55, 0.3);
    duvet.castShadow = true;
    duvet.receiveShadow = true;
    bed.add(duvet);
    // 被子翻折部分
    addBox(bed, 1.95, 0.06, 0.35, Mat.white, 0, 0.52, -0.55);
    // 两个枕头
    const pillowGeo = new THREE.BoxGeometry(0.5, 0.12, 0.72, 4, 3, 4);
    for (const px of [-0.5, 0.5]) {
        const pillow = new THREE.Mesh(pillowGeo, Mat.white);
        pillow.position.set(px, 0.54, -0.72);
        pillow.rotation.x = -0.08;
        pillow.castShadow = true;
        bed.add(pillow);
    }
    // 靠垫（中性色）
    const throwGeo = new THREE.BoxGeometry(0.35, 0.1, 0.35, 3, 2, 3);
    const throwPillow = new THREE.Mesh(throwGeo, new THREE.MeshStandardMaterial({
        color: 0xb0a898, roughness: 0.7
    }));
    throwPillow.position.set(-0.7, 0.56, -0.4);
    throwPillow.rotation.z = 0.3;
    throwPillow.rotation.x = -0.1;
    throwPillow.castShadow = true;
    bed.add(throwPillow);
    bed.position.set(2.6, 0, -2.5);
    room.add(bed);

    // -- 床头柜 x2（增强版）--
    function addNightstand(x, z) {
        const ns = new THREE.Group();
        addBox(ns, 0.52, 0.5, 0.45, Mat.woodLight, 0, 0.25, 0);
        addBox(ns, 0.52, 0.04, 0.45, Mat.woodDark, 0, 0.5, 0);
        // 抽屉
        addBox(ns, 0.4, 0.14, 0.4, Mat.wood, 0, 0.3, 0.01);
        // 抽屉把手
        const pullGeo = new THREE.CylinderGeometry(0.02, 0.02, 0.1, 8);
        const pull = new THREE.Mesh(pullGeo, Mat.metal);
        pull.rotation.x = Math.PI / 2;
        pull.position.set(0, 0.3, 0.23);
        pull.castShadow = true;
        ns.add(pull);
        // 台灯
        const poleGeo = new THREE.CylinderGeometry(0.025, 0.03, 0.38, 8);
        const tPole = new THREE.Mesh(poleGeo, Mat.metal);
        tPole.position.set(0, 0.75, 0.05);
        tPole.castShadow = true;
        ns.add(tPole);
        const lShadeGeo = new THREE.CylinderGeometry(0.08, 0.13, 0.25, 16);
        const lShadeMat = new THREE.MeshStandardMaterial({
            color: 0xfff6e8, roughness: 0.5, emissive: 0x110800, emissiveIntensity: 0.1
        });
        const lShade = new THREE.Mesh(lShadeGeo, lShadeMat);
        lShade.position.set(0, 0.98, 0.05);
        lShade.castShadow = true;
        ns.add(lShade);
        ns.position.set(x, 0, z);
        return ns;
    }
    room.add(addNightstand(1.6, -3.5));
    room.add(addNightstand(3.6, -3.5));

    // -- 衣柜（到顶 + 纹理）--
    const wardrobe = new THREE.Group();
    addBox(wardrobe, 1.85, 2.25, 0.65, Mat.wood, 0, 1.12, 0);
    // 柜门分割
    addBox(wardrobe, 0.03, 2.15, 0.58, Mat.woodDark, 0, 1.12, 0.02);
    // 把手（拉丝金属效果）
    const handleGeo = new THREE.CylinderGeometry(0.015, 0.015, 0.22, 8);
    for (const hx of [0.35, -0.35]) {
        const handle = new THREE.Mesh(handleGeo, Mat.metal);
        handle.rotation.x = Math.PI / 2;
        handle.position.set(hx, 1.12, 0.34);
        wardrobe.add(handle);
    }
    wardrobe.position.set(3.0, 0, -1.0);
    room.add(wardrobe);

    // -- 地毯 --
    const rug = new THREE.Mesh(
        new THREE.PlaneGeometry(1.7, 2.1),
        Mat.carpetBed
    );
    rug.rotation.x = -Math.PI / 2;
    rug.position.set(2.6, 0.008, -2.0);
    rug.receiveShadow = true;
    room.add(rug);

    // -- 梳妆台（角落）--
    const vanity = new THREE.Group();
    addBox(vanity, 0.75, 0.5, 0.38, Mat.woodLight, 0, 0.25, 0);
    addBox(vanity, 0.8, 0.03, 0.4, Mat.woodDark, 0, 0.5, 0);
    // 镜子
    addBox(vanity, 0.45, 0.5, 0.05, Mat.glass, 0, 0.8, -0.18);
    addBox(vanity, 0.48, 0.54, 0.02, Mat.white, 0, 0.8, -0.2);
    // 凳子
    const stoolSeatGeo = new THREE.CylinderGeometry(0.18, 0.2, 0.05, 16);
    const stoolSeat = new THREE.Mesh(stoolSeatGeo, Mat.leather);
    stoolSeat.position.set(0, 0.48, 0.3);
    stoolSeat.castShadow = true;
    vanity.add(stoolSeat);
    const stoolLegGeo = new THREE.CylinderGeometry(0.02, 0.02, 0.4, 8);
    const stoolLeg = new THREE.Mesh(stoolLegGeo, Mat.metal);
    stoolLeg.position.set(0, 0.22, 0.3);
    stoolLeg.castShadow = true;
    vanity.add(stoolLeg);
    vanity.position.set(4.1, 0, -3.0);
    room.add(vanity);

    room.position.set(0, 0, 0);
    return room;
}

// ==================== 次卧家具（增强版）====================
function buildSecondBedroom() {
    const room = new THREE.Group();

    // -- 单人床 --
    const bed = new THREE.Group();
    addBox(bed, 1.35, 0.28, 2.15, Mat.wood, 0, 0.14, 0);
    addBox(bed, 1.4, 0.06, 2.2, Mat.woodDark, 0, 0.28, 0);
    addBox(bed, 1.45, 0.8, 0.1, Mat.woodDark, 0, 0.55, -1.05);
    addBox(bed, 1.25, 0.16, 1.95, Mat.mattress, 0, 0.36, 0.05);
    const duvetGeo = new THREE.BoxGeometry(1.3, 0.08, 1.45, 5, 2, 5);
    const duvet = new THREE.Mesh(duvetGeo, Mat.sheet);
    duvet.position.set(0, 0.47, 0.28);
    duvet.castShadow = true;
    duvet.receiveShadow = true;
    bed.add(duvet);
    addBox(bed, 1.3, 0.05, 0.3, Mat.white, 0, 0.44, -0.58);
    const pillowGeo = new THREE.BoxGeometry(0.58, 0.1, 0.58, 4, 3, 4);
    const pillow = new THREE.Mesh(pillowGeo, Mat.white);
    pillow.position.set(0, 0.48, -0.68);
    pillow.rotation.x = -0.08;
    pillow.castShadow = true;
    bed.add(pillow);
    bed.position.set(-2.6, 0, 2.5);
    room.add(bed);

    // -- 书桌（增强版）--
    const desk = new THREE.Group();
    addBox(desk, 1.25, 0.04, 0.6, Mat.woodLight, 0, 0.76, 0);
    // 金属桌腿
    const dLegGeo = new THREE.CylinderGeometry(0.025, 0.025, 0.72, 8);
    for (const [lx, lz] of [[-0.52, -0.25], [0.52, -0.25], [-0.52, 0.25], [0.52, 0.25]]) {
        const dLeg = new THREE.Mesh(dLegGeo, Mat.metal);
        dLeg.position.set(lx, 0.36, lz);
        dLeg.castShadow = true;
        desk.add(dLeg);
    }
    // 椅子（更真实）
    const chairSeatGeo = new THREE.BoxGeometry(0.45, 0.05, 0.45, 3, 2, 3);
    const chairSeat = new THREE.Mesh(chairSeatGeo, Mat.leather);
    chairSeat.position.set(0, 0.46, 0.48);
    chairSeat.castShadow = true;
    desk.add(chairSeat);
    const chairLegGeo = new THREE.CylinderGeometry(0.02, 0.02, 0.42, 8);
    for (const [clx, clz] of [[-0.18, 0.32], [0.18, 0.32], [-0.18, 0.62], [0.18, 0.62]]) {
        const cLeg = new THREE.Mesh(chairLegGeo, Mat.metal);
        cLeg.position.set(clx, 0.21, clz);
        cLeg.castShadow = true;
        desk.add(cLeg);
    }
    const chairBackGeo = new THREE.BoxGeometry(0.42, 0.35, 0.05, 3, 3, 2);
    const chairBack = new THREE.Mesh(chairBackGeo, Mat.leather);
    chairBack.position.set(0, 0.68, 0.7);
    chairBack.castShadow = true;
    desk.add(chairBack);
    // 桌上物品
    const smallBookGeo = new THREE.BoxGeometry(0.14, 0.02, 0.2, 2, 1, 2);
    for (let i = 0; i < 2; i++) {
        const sBook = new THREE.Mesh(smallBookGeo, new THREE.MeshStandardMaterial({
            color: new THREE.Color().setHSL(0.55 + i * 0.3, 0.5, 0.3 + i * 0.15),
            roughness: 0.5
        }));
        sBook.position.set(0.35 + i * 0.06, 0.79, 0.08);
        sBook.rotation.y = 0.3;
        sBook.castShadow = true;
        desk.add(sBook);
    }
    desk.position.set(-3.8, 0, 1.0);
    room.add(desk);

    // -- 小衣柜 --
    const wardrobe = new THREE.Group();
    addBox(wardrobe, 1.2, 2.1, 0.6, Mat.wood, 0, 1.05, 0);
    addBox(wardrobe, 0.03, 2.0, 0.54, Mat.woodDark, 0, 1.05, 0);
    const whGeo = new THREE.CylinderGeometry(0.015, 0.015, 0.15, 8);
    for (const wx of [0.22, -0.22]) {
        const wh = new THREE.Mesh(whGeo, Mat.metal);
        wh.rotation.x = Math.PI / 2;
        wh.position.set(wx, 1.05, 0.32);
        wardrobe.add(wh);
    }
    wardrobe.position.set(-4.0, 0, 3.6);
    room.add(wardrobe);

    // -- 地毯 --
    const rug = new THREE.Mesh(
        new THREE.PlaneGeometry(1.2, 1.6),
        Mat.carpetBed
    );
    rug.rotation.x = -Math.PI / 2;
    rug.position.set(-2.6, 0.008, 2.0);
    rug.receiveShadow = true;
    room.add(rug);

    room.position.set(0, 0, 0);
    return room;
}

// ==================== 卫生间（增强版）====================
function buildBathroom() {
    const room = new THREE.Group();

    // -- 马桶 --
    const toilet = new THREE.Group();
    const baseGeo = new THREE.CylinderGeometry(0.3, 0.36, 0.48, 20);
    const baseMat = new THREE.MeshStandardMaterial({ color: 0xfafaf8, roughness: 0.12, metalness: 0.05 });
    const base = new THREE.Mesh(baseGeo, baseMat);
    base.position.set(0, 0.24, 0);
    base.castShadow = true;
    base.receiveShadow = true;
    toilet.add(base);
    const seatGeo = new THREE.TorusGeometry(0.28, 0.06, 10, 20);
    const seat = new THREE.Mesh(seatGeo, baseMat);
    seat.rotation.x = Math.PI / 2;
    seat.position.set(0, 0.5, 0);
    seat.castShadow = true;
    toilet.add(seat);
    addBox(toilet, 0.42, 0.52, 0.2, Mat.white, 0, 0.72, 0.26);
    const btnGeo = new THREE.CylinderGeometry(0.04, 0.04, 0.03, 12);
    const flushBtn = new THREE.Mesh(btnGeo, Mat.metal);
    flushBtn.position.set(0, 0.84, 0.34);
    toilet.add(flushBtn);
    toilet.position.set(1.5, 0, 2.2);
    room.add(toilet);

    // -- 洗手台（大理石台面） --
    const vanity = new THREE.Group();
    addBox(vanity, 1.05, 0.5, 0.45, Mat.wood, 0, 0.25, 0);
    addBox(vanity, 1.1, 0.04, 0.5, Mat.marble, 0, 0.52, 0);
    const basinGeo = new THREE.CylinderGeometry(0.16, 0.14, 0.08, 20);
    const basin = new THREE.Mesh(basinGeo, new THREE.MeshStandardMaterial({
        color: 0xfafaf8, roughness: 0.05, metalness: 0.1
    }));
    basin.position.set(0, 0.52, 0);
    basin.castShadow = true;
    vanity.add(basin);
    // 水龙头
    const faucetBaseGeo = new THREE.CylinderGeometry(0.025, 0.03, 0.15, 12);
    const faucetBase = new THREE.Mesh(faucetBaseGeo, Mat.metal);
    faucetBase.position.set(0, 0.62, -0.1);
    vanity.add(faucetBase);
    const faucetArmGeo = new THREE.CylinderGeometry(0.018, 0.018, 0.2, 8);
    const faucetArm = new THREE.Mesh(faucetArmGeo, Mat.metal);
    faucetArm.rotation.x = Math.PI / 2;
    faucetArm.position.set(0.08, 0.67, -0.1);
    vanity.add(faucetArm);
    // 镜子 + 背光效果
    addBox(vanity, 0.72, 0.85, 0.05, Mat.glass, 0, 1.12, -0.2);
    const mirrorFrameGeo = new THREE.BoxGeometry(0.78, 0.91, 0.03);
    const mirrorFrame = new THREE.Mesh(mirrorFrameGeo, Mat.white);
    mirrorFrame.position.set(0, 1.12, -0.22);
    vanity.add(mirrorFrame);
    // 镜子边缘灯带
    const mirrorGlowGeo = new THREE.BoxGeometry(0.78, 0.03, 0.04);
    const mirrorGlowMat = new THREE.MeshStandardMaterial({
        color: 0xffffff, emissive: 0xffeecc, emissiveIntensity: 0.2, roughness: 0.3
    });
    const mirrorGlow = new THREE.Mesh(mirrorGlowGeo, mirrorGlowMat);
    mirrorGlow.position.set(0, 0.66, -0.21);
    vanity.add(mirrorGlow);
    vanity.position.set(3.8, 0, 3.6);
    room.add(vanity);

    // -- 淋浴间 --
    const shower = new THREE.Group();
    addBox(shower, 1.0, 0.04, 1.0, Mat.tile, 0, 0.02, 0);
    // 玻璃隔断
    const sGlassGeo = new THREE.BoxGeometry(0.03, 2.05, 1.0);
    const sGlass1 = new THREE.Mesh(sGlassGeo, Mat.glass);
    sGlass1.position.set(0.52, 1.02, 0);
    sGlass1.castShadow = true;
    shower.add(sGlass1);
    const sGlass2 = new THREE.Mesh(new THREE.BoxGeometry(1.0, 2.05, 0.03), Mat.glass);
    sGlass2.position.set(0, 1.02, 0.52);
    sGlass2.castShadow = true;
    shower.add(sGlass2);
    // 花洒
    const pipeGeo = new THREE.CylinderGeometry(0.02, 0.02, 0.4, 8);
    const pipe = new THREE.Mesh(pipeGeo, Mat.metal);
    pipe.position.set(0, 2.15, 0);
    pipe.castShadow = true;
    shower.add(pipe);
    const headGeo = new THREE.CylinderGeometry(0.12, 0.2, 0.06, 20);
    const head = new THREE.Mesh(headGeo, Mat.metal);
    head.position.set(0, 2.35, 0);
    head.castShadow = true;
    shower.add(head);
    shower.position.set(3.8, 0, 1.2);
    room.add(shower);

    // -- 毛巾架（带毛巾）--
    const rack = new THREE.Group();
    const railGeo = new THREE.CylinderGeometry(0.02, 0.02, 0.7, 10);
    const rail = new THREE.Mesh(railGeo, Mat.metal);
    rail.rotation.z = Math.PI / 2;
    rack.add(rail);
    // 毛巾
    const towelGeo = new THREE.PlaneGeometry(0.25, 0.45);
    const towelMat = new THREE.MeshStandardMaterial({
        map: Tex.fabricGrey, roughness: 0.85, side: THREE.DoubleSide
    });
    const towel = new THREE.Mesh(towelGeo, towelMat);
    towel.position.set(0.12, -0.2, 0);
    rack.add(towel);
    rack.position.set(1.2, 1.65, 3.8);
    room.add(rack);

    // -- 浴巾架 --
    const towelRack2 = new THREE.Group();
    const rail2Geo = new THREE.CylinderGeometry(0.018, 0.018, 0.5, 8);
    const rail2 = new THREE.Mesh(rail2Geo, Mat.metal);
    rail2.rotation.z = Math.PI / 2;
    towelRack2.add(rail2);
    const towel2Geo = new THREE.PlaneGeometry(0.2, 0.35);
    const towel2 = new THREE.Mesh(towel2Geo, new THREE.MeshStandardMaterial({
        color: 0xf0e8d8, roughness: 0.85, side: THREE.DoubleSide
    }));
    towel2.position.set(-0.1, -0.16, 0);
    towelRack2.add(towel2);
    towelRack2.position.set(4.0, 1.6, 1.6);
    room.add(towelRack2);

    room.position.set(0, 0, 0);
    return room;
}

// ==================== 实例化所有家具 ====================
scene.add(buildLivingRoom());
scene.add(buildMasterBedroom());
scene.add(buildSecondBedroom());
scene.add(buildBathroom());

// ----- 走廊：简约长地毯（N-S走向）-----
{
    const hallRugGeo = new THREE.PlaneGeometry(1.0, 3.0);
    const hallRugTex = Tex.rugBedroom;
    hallRugTex.repeat.set(1, 3);
    const hallRug = new THREE.Mesh(hallRugGeo, new THREE.MeshStandardMaterial({
        map: hallRugTex, roughness: 0.95, side: THREE.DoubleSide
    }));
    hallRug.rotation.x = -Math.PI / 2;
    hallRug.position.set(0, 0.01, 0);
    hallRug.receiveShadow = true;
    scene.add(hallRug);
}

// ----- 客厅吸顶灯（极简嵌入筒灯）-----
{
    const trimGeo = new THREE.CylinderGeometry(0.22, 0.22, 0.03, 32);
    const trimMat = new THREE.MeshStandardMaterial({ color: 0x2a2a2a, roughness: 0.3, metalness: 0.5 });
    const trim = new THREE.Mesh(trimGeo, trimMat);
    trim.position.set(-2.6, WALL_H - 0.02, -2.6);
    scene.add(trim);
    const lightGeo = new THREE.CylinderGeometry(0.18, 0.18, 0.02, 32);
    const lightMat = new THREE.MeshStandardMaterial({
        color: 0xfffaf0, roughness: 0.1, emissive: 0xffffff, emissiveIntensity: 0.5
    });
    const lightDisc = new THREE.Mesh(lightGeo, lightMat);
    lightDisc.position.set(-2.6, WALL_H - 0.03, -2.6);
    scene.add(lightDisc);
}

// ----- 主卧吸顶灯 -----
{
    const trimGeo = new THREE.CylinderGeometry(0.18, 0.18, 0.03, 32);
    const trim = new THREE.Mesh(trimGeo, new THREE.MeshStandardMaterial({ color: 0x2a2a2a, roughness: 0.3, metalness: 0.5 }));
    trim.position.set(2.6, WALL_H - 0.02, -2.6);
    scene.add(trim);
    const lightGeo = new THREE.CylinderGeometry(0.14, 0.14, 0.02, 32);
    const lightDisc = new THREE.Mesh(lightGeo, new THREE.MeshStandardMaterial({
        color: 0xfffaf0, roughness: 0.1, emissive: 0xffffff, emissiveIntensity: 0.4
    }));
    lightDisc.position.set(2.6, WALL_H - 0.03, -2.6);
    scene.add(lightDisc);
}

// ----- 次卧吸顶灯 -----
{
    const trimGeo = new THREE.CylinderGeometry(0.16, 0.16, 0.03, 32);
    const trim = new THREE.Mesh(trimGeo, new THREE.MeshStandardMaterial({ color: 0x2a2a2a, roughness: 0.3, metalness: 0.5 }));
    trim.position.set(-2.6, WALL_H - 0.02, 2.6);
    scene.add(trim);
    const lightGeo = new THREE.CylinderGeometry(0.12, 0.12, 0.02, 32);
    const lightDisc = new THREE.Mesh(lightGeo, new THREE.MeshStandardMaterial({
        color: 0xfffaf0, roughness: 0.1, emissive: 0xffffff, emissiveIntensity: 0.4
    }));
    lightDisc.position.set(-2.6, WALL_H - 0.03, 2.6);
    scene.add(lightDisc);
}

// ----- 卫生间吸顶灯 -----
{
    const trimGeo = new THREE.CylinderGeometry(0.14, 0.14, 0.03, 32);
    const trim = new THREE.Mesh(trimGeo, new THREE.MeshStandardMaterial({ color: 0x2a2a2a, roughness: 0.3, metalness: 0.5 }));
    trim.position.set(2.6, WALL_H - 0.02, 2.6);
    scene.add(trim);
    const lightGeo = new THREE.CylinderGeometry(0.1, 0.1, 0.02, 32);
    const lightDisc = new THREE.Mesh(lightGeo, new THREE.MeshStandardMaterial({
        color: 0xfffaf0, roughness: 0.1, emissive: 0xffffff, emissiveIntensity: 0.4
    }));
    lightDisc.position.set(2.6, WALL_H - 0.03, 2.6);
    scene.add(lightDisc);
}

// ╔══════════════════════════════════════════════════════════════╗
// ║  第3区：IoT设备3D实体  统一工厂模式                          ║
// ╚══════════════════════════════════════════════════════════════╝

// ----- 光晕纹理生成器 -----
function createGlowTexture(innerColor, size = 128) {
    const canvas = document.createElement('canvas');
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d');
    const half = size / 2;
    const gradient = ctx.createRadialGradient(half, half, half * 0.05, half, half, half);
    gradient.addColorStop(0, innerColor);
    gradient.addColorStop(0.25, innerColor);
    gradient.addColorStop(0.6, 'rgba(255,255,255,0.05)');
    gradient.addColorStop(1, 'rgba(0,0,0,0)');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, size, size);
    const tex = new THREE.CanvasTexture(canvas);
    tex.needsUpdate = true;
    return tex;
}

// ----- 1. 人体红外感应 Status_body -----
const gateSensorPos = new THREE.Vector3(0, 2.4, -4.4);
const sensorMat = new THREE.MeshStandardMaterial({
    color: 0x4466aa, emissive: 0x004466, roughness: 0.3, metalness: 0.4
});
const gateSphere = new THREE.Mesh(new THREE.SphereGeometry(0.22, 16, 16), sensorMat);
gateSphere.position.copy(gateSensorPos);
gateSphere.castShadow = true;
scene.add(gateSphere);

const gateRingMat = new THREE.MeshStandardMaterial({
    color: 0x4466aa, emissive: 0x004466, transparent: true, opacity: 0.6
});
const gateRing = new THREE.Mesh(new THREE.TorusGeometry(0.4, 0.04, 8, 24), gateRingMat);
gateRing.position.copy(gateSensorPos);
gateRing.rotation.x = Math.PI / 2;
scene.add(gateRing);

// PIR检测锥形区域（半透明锥体，表示感应范围）
const pirConeGeo = new THREE.ConeGeometry(0.8, 1.0, 16, 1, true);
const pirConeMat = new THREE.MeshBasicMaterial({
    color: 0xff4422, transparent: true, opacity: 0, side: THREE.DoubleSide, depthWrite: false
});
const pirCone = new THREE.Mesh(pirConeGeo, pirConeMat);
pirCone.position.set(0, 1.6, -4.4);
pirCone.rotation.x = Math.PI; // 尖端朝下
scene.add(pirCone);

// 人体感应标签（3D空间内）
const bodyLabelDiv = document.createElement('div');
bodyLabelDiv.style.cssText = 'color:#ff6666;font-size:12px;font-weight:bold;background:rgba(0,0,0,0.6);padding:2px 8px;border-radius:10px;pointer-events:none;white-space:nowrap;';
bodyLabelDiv.textContent = '无人';
const bodyLabel = new CSS2DObject(bodyLabelDiv);
bodyLabel.position.set(0, 2.8, -4.4);
scene.add(bodyLabel);

// ----- 2. 蜂鸣器 Status_beeper -----
const buzzerPos = new THREE.Vector3(0.6, 2.4, -4.4);
const buzzerMat = new THREE.MeshStandardMaterial({
    color: 0x333333, emissive: 0x111111, roughness: 0.4, metalness: 0.6
});
const buzzerMesh = new THREE.Mesh(new THREE.CylinderGeometry(0.18, 0.18, 0.25, 12), buzzerMat);
buzzerMesh.position.copy(buzzerPos);
buzzerMesh.castShadow = true;
scene.add(buzzerMesh);

const buzzerRingMat = new THREE.MeshStandardMaterial({
    color: 0x333333, emissive: 0x111111, transparent: true, opacity: 0.5
});
const buzzerRing = new THREE.Mesh(new THREE.TorusGeometry(0.22, 0.03, 8, 20), buzzerRingMat);
buzzerRing.position.copy(buzzerPos);
scene.add(buzzerRing);

// ----- 报警扩散波纹池（动态创建/销毁）-----
const alarmWaves = [];

function spawnAlarmWave() {
    const geo = new THREE.TorusGeometry(0.25, 0.04, 8, 32);
    const mat = new THREE.MeshBasicMaterial({
        color: 0xff2222, transparent: true, opacity: 0.85, depthWrite: false
    });
    const wave = new THREE.Mesh(geo, mat);
    wave.position.copy(buzzerPos);
    wave.rotation.x = Math.PI / 2;
    wave.userData = { life: 1.5, maxLife: 1.5, startScale: 0.4 };
    wave.scale.set(0.4, 0.4, 0.4);
    wave.renderOrder = 999;
    scene.add(wave);
    alarmWaves.push(wave);
}

function cleanupAlarmWaves() {
    for (let i = alarmWaves.length - 1; i >= 0; i--) {
        const w = alarmWaves[i];
        scene.remove(w);
        w.geometry.dispose();
        w.material.dispose();
    }
    alarmWaves.length = 0;
}

// ----- 3/4/5. 三色灯具（客厅绿灯 / 主卧红灯 / 卫生间黄灯）-----
function createLightFixture(position, colorHex, label) {
    const fixture = { _label: label };

    // PointLight — 物理光照：intensity 单位 cd，需要较大值
    const light = new THREE.PointLight(colorHex, 0, 25, 0.6);
    light.position.copy(position);
    light.castShadow = false;
    scene.add(light);
    fixture.light = light;
    console.log(`[createLight] ${label} PointLight created at`, position.toArray(), 'colorHex=' + colorHex.toString(16));

    // 灯泡实体
    const bulbMat = new THREE.MeshStandardMaterial({
        color: 0xfff8e0, emissive: new THREE.Color(colorHex).multiplyScalar(0.2),
        roughness: 0.2, metalness: 0.1
    });
    const bulb = new THREE.Mesh(new THREE.SphereGeometry(0.2, 16, 16), bulbMat);
    bulb.position.copy(position);
    bulb.castShadow = true;
    scene.add(bulb);
    fixture.bulb = bulb;
    fixture.bulbMat = bulbMat;

    // 光晕 Sprite
    const glowTex = createGlowTexture('#' + new THREE.Color(colorHex).getHexString());
    const glowMat = new THREE.SpriteMaterial({
        map: glowTex, blending: THREE.AdditiveBlending,
        transparent: true, opacity: 0, depthWrite: false, depthTest: true
    });
    const glowSprite = new THREE.Sprite(glowMat);
    glowSprite.position.copy(position);
    glowSprite.scale.set(1.8, 1.8, 1);
    glowSprite.renderOrder = 998;
    scene.add(glowSprite);
    fixture.glow = glowSprite;
    fixture.glowMat = glowMat;

    // 灯光锥体（向下照射的体积光模拟）
    const coneGeo = new THREE.CylinderGeometry(0.05, 1.2, 1.6, 16, 1, true);
    const coneMat = new THREE.MeshBasicMaterial({
        color: colorHex, transparent: true, opacity: 0, side: THREE.DoubleSide, depthWrite: false
    });
    const lightCone = new THREE.Mesh(coneGeo, coneMat);
    lightCone.position.copy(position);
    lightCone.position.y -= 0.9;
    lightCone.renderOrder = 997;
    scene.add(lightCone);
    fixture.cone = lightCone;
    fixture.coneMat = coneMat;

    // 目标值（动画系统用）
    fixture.targetIntensity = 0;
    fixture.targetConeOpacity = 0;
    fixture.targetGlowOpacity = 0;
    fixture.targetEmissive = new THREE.Color(colorHex).multiplyScalar(0.2);

    return fixture;
}

const ledGreen  = createLightFixture(new THREE.Vector3(-2.6, 2.3, -2.6), 0x00ff44, '客厅绿灯');
const ledRed    = createLightFixture(new THREE.Vector3(2.6, 2.3, -2.6), 0xff3333, '主卧红灯');
const ledYellow = createLightFixture(new THREE.Vector3(2.6, 2.3, 2.6), 0xffdd00, '卫生间黄灯');

// ----- 6. 温湿度传感器实体 -----
const tempHumiPos = new THREE.Vector3(-2.6, 2.0, -2.0);
const thMat = new THREE.MeshStandardMaterial({
    color: 0x22bb88, emissive: 0x116644, roughness: 0.3, metalness: 0.3
});
const thMesh = new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.3, 0.15), thMat);
thMesh.position.copy(tempHumiPos);
thMesh.castShadow = true;
scene.add(thMesh);

// 温湿度3D标签
const thLabelDiv = document.createElement('div');
thLabelDiv.style.cssText = 'color:#22dd99;font-size:11px;background:rgba(0,0,0,0.55);padding:2px 8px;border-radius:8px;pointer-events:none;white-space:nowrap;';
thLabelDiv.textContent = '25.0℃ / 50.0%';
const thLabel3D = new CSS2DObject(thLabelDiv);
thLabel3D.position.set(-2.6, 2.4, -2.0);
scene.add(thLabel3D);

// ╔══════════════════════════════════════════════════════════════╗
// ║  第4区：状态管理器  字段映射 / 设备状态 / 联动规则引擎       ║
// ╚══════════════════════════════════════════════════════════════╝

// ----- 设备状态存储 -----
const deviceMap = {
    bodySensor: { sphere: gateSphere, ring: gateRing, cone: pirCone, label: bodyLabel, state: false },
    beeper:     { mesh: buzzerMesh, ring: buzzerRing, alarm: false, lastWaveTime: 0 },
    ledGreen:   { ...ledGreen, state: false },
    ledRed:     { ...ledRed, state: false },
    ledYellow:  { ...ledYellow, state: false },
    tempHumi:   { mesh: thMesh, label3D: thLabel3D }
};

let currentTemp = 25.0;
let currentHumi = 50.0;
let _linkageLocked = false;
let _alarmWaveTimer = 0;

// 挂载全局
window.deviceMap = deviceMap;
window.currentTemp = currentTemp;
window.currentHumi = currentHumi;

// 验证：打印三灯关键属性
console.log('[init] deviceMap 灯光验证:',
    'ledGreen:', { hasLight: !!deviceMap.ledGreen.light, hasBulb: !!deviceMap.ledGreen.bulb, targetIntensity: deviceMap.ledGreen.targetIntensity },
    'ledRed:',   { hasLight: !!deviceMap.ledRed.light, hasBulb: !!deviceMap.ledRed.bulb, targetIntensity: deviceMap.ledRed.targetIntensity },
    'ledYellow:',{ hasLight: !!deviceMap.ledYellow.light, hasBulb: !!deviceMap.ledYellow.bulb, targetIntensity: deviceMap.ledYellow.targetIntensity }
);

// ----- UI DOM缓存 -----
const uiDom = {
    ledBodySensor:  document.getElementById('led-body-sensor'),
    statusBodySensor: document.getElementById('status-body-sensor'),
    ledBeeper:      document.getElementById('led-beeper'),
    statusBeeper:   document.getElementById('status-beeper'),
    ledLedGreen:    document.getElementById('led-led-green'),
    statusLedGreen: document.getElementById('status-led-green'),
    ledLedRed:      document.getElementById('led-led-red'),
    statusLedRed:   document.getElementById('status-led-red'),
    ledLedYellow:   document.getElementById('led-led-yellow'),
    statusLedYellow: document.getElementById('status-led-yellow'),
    tip:            document.getElementById('status-tip'),
    tempValDom:     document.getElementById('temp-val'),
    humiValDom:     document.getElementById('humi-val'),
    statusTempHumi: document.getElementById('status-temp-humi'),
    tempHumiPanel:  document.getElementById('temp-humi-panel')
};

// ----- 刷新UI指示灯 -----
function refreshUI(deviceId, state) {
    switch (deviceId) {
        case 'bodySensor':
            uiDom.ledBodySensor.className = state ? 'led alarm' : 'led off';
            uiDom.statusBodySensor.textContent = state ? '有人闯入!' : '无人';
            bodyLabelDiv.textContent = state ? '⚠ 有人!' : '无人';
            bodyLabelDiv.style.color = state ? '#ff4444' : '#ff6666';
            uiDom.tip.textContent = state ? '🚨 人体红外检测到人员闯入！自动触发蜂鸣器报警' : '🚪 入户区域安全';
            uiDom.tip.style.color = state ? '#ff6644' : '#0ff';
            uiDom.tip.style.borderColor = state ? '#ff6644' : '#0ff';
            break;
        case 'beeper':
            uiDom.ledBeeper.className = state ? 'led alarm' : 'led off';
            uiDom.statusBeeper.textContent = state ? '蜂鸣中!' : '安全';
            if (!deviceMap.bodySensor.state) {
                uiDom.tip.textContent = state ? '🔔 蜂鸣器触发报警 (手动/高温/高湿)' : '🔔 报警器待机';
                uiDom.tip.style.color = state ? '#ff2222' : '#0ff';
                uiDom.tip.style.borderColor = state ? '#ff2222' : '#0ff';
            }
            break;
        case 'ledGreen':
            uiDom.ledLedGreen.className = state ? 'led on' : 'led off';
            uiDom.statusLedGreen.textContent = state ? '开' : '关';
            break;
        case 'ledRed':
            uiDom.ledLedRed.className = state ? 'led red-on' : 'led off';
            uiDom.statusLedRed.textContent = state ? '开' : '关';
            break;
        case 'ledYellow':
            uiDom.ledLedYellow.className = state ? 'led yellow-on' : 'led off';
            uiDom.statusLedYellow.textContent = state ? '开' : '关';
            break;
    }
}

// ----- 核心：设备状态切换（驱动3D + UI + 联动）-----
window.setDeviceState = function (deviceId, state) {
    const dev = deviceMap[deviceId];
    if (!dev) return console.error('[setDeviceState] 未知设备：' + deviceId);

    const prevState = dev.state !== undefined ? dev.state : dev.alarm;
    dev.state = state;

    // ── 三色灯光：设置目标值，动画系统平滑过渡 ──
    if (['ledGreen', 'ledRed', 'ledYellow'].includes(deviceId)) {
        const onIntensity = deviceId === 'ledGreen' ? 8 :
                            deviceId === 'ledRed' ? 7 : 6;
        const colorHex = deviceId === 'ledGreen' ? 0x00ff44 :
                         deviceId === 'ledRed' ? 0xff3333 : 0xffdd00;
        const dimColor = new THREE.Color(colorHex).multiplyScalar(0.15);

        dev.targetIntensity = state ? onIntensity : 0;
        dev.targetConeOpacity = state ? 0.15 : 0;
        dev.targetGlowOpacity = state ? 0.65 : 0;
        dev.targetEmissive = state ? new THREE.Color(colorHex) : dimColor;
    }

    // ── 人体感应 ──
    if (deviceId === 'bodySensor') {
        const onColor = 0xff4422;
        const offColor = 0x4466aa;
        const colorHex = state ? onColor : offColor;
        dev.sphere.material.color.setHex(colorHex);
        dev.sphere.material.emissive.setHex(colorHex);
        dev.ring.material.color.setHex(colorHex);
        dev.ring.material.emissive.setHex(colorHex);
        dev.cone.material.opacity = state ? 0.3 : 0;
    }

    // ── 蜂鸣器 ──
    if (deviceId === 'beeper') {
        dev.alarm = state;
        const alarmColor = state ? 0xff0000 : 0x333333;
        dev.mesh.material.color.setHex(alarmColor);
        dev.mesh.material.emissive.setHex(alarmColor);
        dev.ring.material.color.setHex(alarmColor);
        dev.ring.material.emissive.setHex(alarmColor);
        if (state) {
            _alarmWaveTimer = 0; // 立即触发首个波纹
        } else {
            cleanupAlarmWaves();
        }
    }

    refreshUI(deviceId, state);

    // ── 联动规则评估 ──
    if (!_linkageLocked && state !== prevState) {
        evaluateLinkageRules(deviceId, state);
    }
};

// ----- 温湿度更新 -----
window.setTempHumi = function (temp, humi) {
    currentTemp = Number(temp).toFixed(1);
    currentHumi = Number(humi).toFixed(1);
    window.currentTemp = currentTemp;
    window.currentHumi = currentHumi;

    // UI更新
    uiDom.tempValDom.textContent = `${currentTemp} ℃`;
    uiDom.humiValDom.textContent = `${currentHumi} %RH`;
    uiDom.statusTempHumi.textContent = `${currentTemp}℃ / ${currentHumi}%RH`;
    thLabelDiv.textContent = `${currentTemp}℃ / ${currentHumi}%`;

    // 传感器实体颜色随温度变化（冷→暖）
    const t = Math.max(0, Math.min(45, currentTemp));
    const coldColor = new THREE.Color(0x2288cc);  // 冷(0°C)
    const warmColor = new THREE.Color(0xff6622);   // 热(45°C)
    const thColor = coldColor.clone().lerp(warmColor, t / 45);
    thMat.color.copy(thColor);
    thMat.emissive.copy(thColor).multiplyScalar(0.5);

    // 温湿度面板警告色
    const panel = uiDom.tempHumiPanel;
    panel.classList.remove('warn-temp', 'warn-humi');
    uiDom.tempValDom.classList.remove('temp-hot');
    uiDom.humiValDom.classList.remove('humi-wet');

    if (currentTemp > 30) {
        panel.classList.add('warn-temp');
        uiDom.tempValDom.classList.add('temp-hot');
    }
    if (currentHumi > 80) {
        panel.classList.add('warn-humi');
        uiDom.humiValDom.classList.add('humi-wet');
    }

    // 联动规则
    evaluateLinkageRules('env');
};

// ----- 联动规则引擎 -----
function evaluateLinkageRules(triggerDevice, newState) {
    if (_linkageLocked) return;
    _linkageLocked = true;

    // 规则1: 人体检测到 → 仅触发蜂鸣器报警
    if (triggerDevice === 'bodySensor' && newState === true) {
        if (!deviceMap.beeper.alarm) {
            window.setDeviceState('beeper', true);
        }
    }

    // 规则2: 高温 > 30℃ → 红灯亮起警告（环境联动，不覆盖手动操作）
    if (triggerDevice === 'env' && currentTemp > 30) {
        if (!deviceMap.ledRed.state) {
            window.setDeviceState('ledRed', true);
        }
    }

    // 规则3: 高湿 > 80% → 黄灯亮起警告（环境联动，不覆盖手动操作）
    if (triggerDevice === 'env' && currentHumi > 80) {
        if (!deviceMap.ledYellow.state) {
            window.setDeviceState('ledYellow', true);
        }
    }

    _linkageLocked = false;
}
window.evaluateLinkageRules = evaluateLinkageRules;

// ╔══════════════════════════════════════════════════════════════╗
// ║  第5区：动画系统  平滑过渡 / 报警波纹 / 脉冲动画             ║
// ╚══════════════════════════════════════════════════════════════╝

const clock = new THREE.Clock();

function updateAnimations(delta) {
    const t = performance.now() / 1000;

    // ── 灯光平滑过渡（intensity / emissive / glow / cone）──
    ['ledGreen', 'ledRed', 'ledYellow'].forEach(id => {
        const dev = deviceMap[id];
        if (!dev.light) return;

        // 光强度 lerp
        const lerpSpeed = 6.0; // 每秒逼近速度
        dev.light.intensity = THREE.MathUtils.lerp(
            dev.light.intensity, dev.targetIntensity || 0, Math.min(1, lerpSpeed * delta)
        );

        // 灯泡 emissive lerp
        if (dev.targetEmissive) {
            dev.bulbMat.emissive.lerp(dev.targetEmissive, Math.min(1, lerpSpeed * delta));
        }

        // 光晕透明度 lerp
        const glowTarget = dev.targetGlowOpacity || 0;
        dev.glowMat.opacity = THREE.MathUtils.lerp(
            dev.glowMat.opacity, glowTarget, Math.min(1, lerpSpeed * delta)
        );
        // 光晕缩放随亮度
        const glowScale = 1.4 + dev.light.intensity * 0.6;
        dev.glow.scale.set(glowScale, glowScale, 1);

        // 灯光锥体 lerp
        const coneTarget = dev.targetConeOpacity || 0;
        dev.coneMat.opacity = THREE.MathUtils.lerp(
            dev.coneMat.opacity, coneTarget, Math.min(1, lerpSpeed * delta)
        );

        // 亮的灯泡发暖光（用 ledGreen 最大亮度 18 做归一化基准）
        const brightness = dev.light.intensity / 18;
        dev.bulbMat.emissiveIntensity = 0.2 + brightness * 1.8;
    });

    // ── 人体感应脉冲动画 ──
    const sensor = deviceMap.bodySensor;
    if (sensor.state) {
        const pulse = 1 + 0.18 * Math.sin(t * 6);
        sensor.ring.scale.set(pulse, pulse, pulse);
        sensor.ring.material.opacity = 0.35 + 0.45 * Math.sin(t * 6 + 1);
        sensor.sphere.material.emissiveIntensity = 0.5 + 0.5 * Math.sin(t * 5);
        // PIR检测锥体呼吸
        const coneAlpha = 0.2 + 0.15 * Math.sin(t * 4);
        sensor.cone.material.opacity = coneAlpha;
        const coneScale = 1 + 0.1 * Math.sin(t * 4);
        sensor.cone.scale.set(coneScale, 1, coneScale);
    } else {
        sensor.ring.scale.lerp(new THREE.Vector3(1, 1, 1), 0.1);
    }

    // ── 蜂鸣器报警脉冲 + 扩散波纹 ──
    const bee = deviceMap.beeper;
    if (bee.alarm) {
        // 蜂鸣器自身脉冲
        const bpulse = 1 + 0.25 * Math.sin(t * 10);
        bee.ring.scale.set(bpulse, bpulse, bpulse);
        bee.ring.material.opacity = 0.3 + 0.5 * Math.sin(t * 10);
        bee.mesh.material.emissiveIntensity = 0.6 + 0.4 * Math.sin(t * 9);

        // 扩散波纹管理
        _alarmWaveTimer += delta;
        if (_alarmWaveTimer >= 0.45) {
            _alarmWaveTimer = 0;
            spawnAlarmWave();
        }
    } else {
        bee.ring.scale.lerp(new THREE.Vector3(1, 1, 1), 0.1);
    }

    // ── 更新所有报警波纹（扩展 + 淡出）──
    for (let i = alarmWaves.length - 1; i >= 0; i--) {
        const wave = alarmWaves[i];
        wave.userData.life -= delta;
        if (wave.userData.life <= 0) {
            scene.remove(wave);
            wave.geometry.dispose();
            wave.material.dispose();
            alarmWaves.splice(i, 1);
        } else {
            const progress = 1 - wave.userData.life / wave.userData.maxLife;
            const scale = wave.userData.startScale + progress * 6;
            wave.scale.set(scale, scale, scale);
            wave.material.opacity = 0.85 * Math.pow(1 - progress, 1.8);
        }
    }

    // ── 温湿度3D标签微呼吸 ──
    const thBreath = 1 + 0.03 * Math.sin(t * 2);
    thMesh.scale.set(thBreath, thBreath, thBreath);
}

// ╔══════════════════════════════════════════════════════════════╗
// ║  第6区：全局API暴露 + 渲染循环 + 自适应                       ║
// ╚══════════════════════════════════════════════════════════════╝

// 初始化全部设备为关闭状态
window.setDeviceState('bodySensor', false);
window.setDeviceState('beeper', false);
window.setDeviceState('ledGreen', false);
window.setDeviceState('ledRed', false);
window.setDeviceState('ledYellow', false);
window.setTempHumi(currentTemp, currentHumi);

// 按钮本地调试事件
// ===== 后端 API 地址 =====
var BACKEND = window.location.hostname ? 'http://' + window.location.hostname + ':8080' : 'http://192.168.198.114:8080';

// ===== 手动操作防抖: 用户操作后 3 秒内不接受 WebSocket 覆盖 =====
var userActionLock = {};  // { deviceId: 过期时间戳 }

/** 向后端发送设备控制指令 */
function sendDeviceCommand(deviceCode, deviceId, action) {
    // 锁定该设备，5秒内 WebSocket 不覆盖 (留足硬件响应+轮询时间)
    userActionLock[deviceId] = Date.now() + 5000;

    // 先立即更新 3D 视觉效果
    if (window.setDeviceState) window.setDeviceState(deviceId, action);

    fetch(BACKEND + '/api/device/control', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ deviceCode: deviceCode, action: action ? 1 : 0, source: 'digital-twin' })
    }).then(r => r.json()).then(d => {
        console.log('[控制] ' + deviceCode + ' → ' + (action ? '开' : '关'), d);
    }).catch(e => console.error('[控制失败]', e));
}

// 供 WebSocket 回调使用: 检查设备是否被用户锁定
window.isDeviceLocked = function(deviceId) {
    return userActionLock[deviceId] && Date.now() < userActionLock[deviceId];
};

document.getElementById('btn-body-sensor').addEventListener('click', () => {
    window.setDeviceState('bodySensor', !deviceMap.bodySensor.state);
    // 人体感应是只读传感器，不控制硬件
});
document.getElementById('btn-beeper').addEventListener('click', () => {
    var newState = !deviceMap.beeper.alarm;
    sendDeviceCommand('buzzer', 'beeper', newState);
});
document.getElementById('btn-led-green').addEventListener('click', () => {
    var newState = !deviceMap.ledGreen.state;
    sendDeviceCommand('led', 'ledGreen', newState);
});
document.getElementById('btn-led-red').addEventListener('click', () => {
    var newState = !deviceMap.ledRed.state;
    sendDeviceCommand('ledRed', 'ledRed', newState);
});
document.getElementById('btn-led-yellow').addEventListener('click', () => {
    var newState = !deviceMap.ledYellow.state;
    sendDeviceCommand('ledYellow', 'ledYellow', newState);
});

// 渲染循环
function animate() {
    const delta = Math.min(clock.getDelta(), 0.1); // 防止大帧跳跃

    updateAnimations(delta);
    controls.update();

    renderer.render(scene, camera);
    labelRenderer.render(scene, camera);

    requestAnimationFrame(animate);
}
animate();

// 窗口自适应
window.addEventListener('resize', () => {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
    labelRenderer.setSize(window.innerWidth, window.innerHeight);
});

// 启动提示
setTimeout(() => {
    uiDom.tip.textContent = '💡 点击场景按钮体验AIoT联动 | 后端调用 DigitalTwin.updateFromHardware(json) 推送数据';
    uiDom.tip.style.color = '#88ddff';
    uiDom.tip.style.borderColor = '#88ddff';
}, 1500);

console.log('🏠 3D数字孪生引擎 v2.0 初始化完成');
console.log('   📐 场景：2室1厅1卫 + 天花板 + 地板分区');
console.log('   💡 设备：人体感应(Status_body) | 蜂鸣器(Status_beeper) | 3色LED灯');
console.log('   🌡️ 环境：温湿度传感器(Data_temp/Data_humi)');
console.log('   🔗 联动：PIR→蜂鸣器 | 高温→红灯 | 高湿→黄灯 | 全部平滑过渡');
console.log('   🌐 API：DigitalTwin.updateFromHardware(json) 接收华为云IoTDA标准格式');
