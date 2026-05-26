import { useEffect, useRef } from 'react';
import * as THREE from 'three';

export default function Globe() {
    const mountRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const mount = mountRef.current;
        if (!mount) return;

        let animId: number;
        const width = 650;
        const height = 650;

        const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
        renderer.setPixelRatio(window.devicePixelRatio || 1);
        renderer.setSize(width, height);
        mount.appendChild(renderer.domElement);

        const scene = new THREE.Scene();
        
        const camera = new THREE.PerspectiveCamera(50, 1, 0.1, 1000);
        camera.position.set(0, 0, 6.5);
        camera.lookAt(0, 0, 0);

        const globeGroup = new THREE.Group();
        globeGroup.position.set(0, 0.3, 0); 
        scene.add(globeGroup);

        const GLOBE_RADIUS = 2.6;
        const sphereGeo = new THREE.SphereGeometry(GLOBE_RADIUS, 64, 64);

        const textureLoader = new THREE.TextureLoader();
        const landTexture = textureLoader.load(
            'https://raw.githubusercontent.com/mrdoob/three.js/dev/examples/textures/planets/earth_specular_2048.jpg',
            () => {
                landMat.needsUpdate = true;
            }
        );

        const oceanMat = new THREE.MeshBasicMaterial({
            color: 0xF5C518, 
            side: THREE.FrontSide
        });
        const oceanMesh = new THREE.Mesh(sphereGeo, oceanMat);
        globeGroup.add(oceanMesh);

        const landMat = new THREE.MeshBasicMaterial({
            color: 0xf2eeed, 
            alphaMap: landTexture, 
            transparent: true,
            side: THREE.FrontSide,
            depthWrite: true
        });

        const landMesh = new THREE.Mesh(sphereGeo, landMat);
        landMesh.scale.set(1.005, 1.005, 1.005);
        globeGroup.add(landMesh);

        let isDragging = false;
        let prevMouseX = 0;
        let prevMouseY = 0;
        let rotationVelX = 0;
        let rotationVelY = 0.002;

        const applyRotation = (velX: number, velY: number) => {
            const qx = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), velX);
            const qy = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(0, 1, 0), velY);
            globeGroup.quaternion.premultiply(qy).premultiply(qx);
        };

        const onMouseDown = (e: MouseEvent) => {
            isDragging = true;
            prevMouseX = e.clientX;
            prevMouseY = e.clientY;
            mount.style.cursor = 'grabbing';
            rotationVelY = 0;
        };

        const onMouseMove = (e: MouseEvent) => {
            if (!isDragging) return;
            const dx = e.clientX - prevMouseX;
            const dy = e.clientY - prevMouseY;
            rotationVelX = dy * 0.002;
            rotationVelY = dx * 0.002;
            applyRotation(rotationVelX, rotationVelY);
            prevMouseX = e.clientX;
            prevMouseY = e.clientY;
        };

        const onMouseUp = () => {
            isDragging = false;
            mount.style.cursor = 'grab';
        };

        mount.addEventListener('mousedown', onMouseDown);
        window.addEventListener('mousemove', onMouseMove);
        window.addEventListener('mouseup', onMouseUp);

        const animate = () => {
            animId = requestAnimationFrame(animate);

            if (!isDragging) {
                rotationVelY = 0.0015;
                applyRotation(0, rotationVelY);
            }

            renderer.render(scene, camera);
        };

        animate();

        return () => {
            cancelAnimationFrame(animId);
            mount.removeEventListener('mousedown', onMouseDown);
            window.removeEventListener('mousemove', onMouseMove);
            window.removeEventListener('mouseup', onMouseUp);
            if (mount.contains(renderer.domElement)) mount.removeChild(renderer.domElement);

            sphereGeo.dispose();
            landMat.dispose();
            oceanMat.dispose();
            landTexture.dispose();
            renderer.dispose();
        };
    }, []);

    return (
        <div
            ref={mountRef}
            style={{
                width: '650px',
                height: '650px',
                cursor: 'grab',
                margin: '0 auto',
                background: 'transparent',
                borderRadius: '50%',
                overflow: 'hidden'
            }}
        />
    );
}