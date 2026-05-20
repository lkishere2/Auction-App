import { useEffect, useRef } from 'react';
import * as THREE from 'three';

const continentData = [
    { lat: [-35, 37], lon: [-17, 51] },
    { lat: [36, 70], lon: [-10, 145] },
    { lat: [1, 70], lon: [60, 150] },
    { lat: [-55, 12], lon: [-82, -35] },
    { lat: [12, 72], lon: [-168, -55] },
    { lat: [-40, -10], lon: [112, 154] },
];

export default function Globe() {
    const mountRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const mount = mountRef.current;
        if (!mount) return;

        let animId: number;
        const width = 1000;
        const height = 1000;

        const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
        renderer.setPixelRatio(window.devicePixelRatio || 1);
        renderer.setClearColor(0x000000, 0);
        renderer.setSize(width, height);
        mount.appendChild(renderer.domElement);

        const scene = new THREE.Scene();
        const camera = new THREE.PerspectiveCamera(50, width / height, 0.1, 1000);
        camera.position.z = 6.5;

        const globeGroup = new THREE.Group();
        globeGroup.position.y = -0.8;
        scene.add(globeGroup);

        const GLOBE_RADIUS = 2.6;
        const DOT_RADIUS = 0.022;
        const RING_COUNT = 65;
        const SPACING = 0.125;

        const dotPositions: THREE.Vector3[] = [];
        const dotQuaternions: THREE.Quaternion[] = [];

        for (let i = 0; i <= RING_COUNT; i++) {
            const phi = (i / RING_COUNT) * Math.PI;
            const ringRadius = Math.sin(phi) * GLOBE_RADIUS;
            const y = Math.cos(phi) * GLOBE_RADIUS;

            const ringCircumference = 2 * Math.PI * ringRadius;
            const dotsInRing = Math.max(1, Math.floor(ringCircumference / SPACING));

            for (let j = 0; j < dotsInRing; j++) {
                const theta = (j / dotsInRing) * Math.PI * 2;

                const lat = 90 - (phi * 180) / Math.PI;
                const lon = (theta * 180) / Math.PI - 180;

                let onLand = false;
                for (const continent of continentData) {
                    if (lat >= continent.lat[0] && lat <= continent.lat[1] &&
                        lon >= continent.lon[0] && lon <= continent.lon[1]) {
                        onLand = true;
                        break;
                    }
                }

                if (onLand) {
                    const x = ringRadius * Math.cos(theta);
                    const z = ringRadius * Math.sin(theta);
                    const pos = new THREE.Vector3(x, y, z);
                    dotPositions.push(pos);

                    const dummy = new THREE.Object3D();
                    dummy.position.copy(pos);
                    dummy.lookAt(pos.clone().multiplyScalar(2));
                    dotQuaternions.push(dummy.quaternion.clone());
                }
            }
        }

        const circleGeo = new THREE.CircleGeometry(DOT_RADIUS, 16);
        const circleMat = new THREE.MeshBasicMaterial({
            color: 0xF5C518,
            side: THREE.FrontSide,
            transparent: true,
            opacity: 0.95
        });

        const instancedMesh = new THREE.InstancedMesh(circleGeo, circleMat, dotPositions.length);
        const dummyObj = new THREE.Object3D();

        for (let i = 0; i < dotPositions.length; i++) {
            dummyObj.position.copy(dotPositions[i]);
            dummyObj.quaternion.copy(dotQuaternions[i]);
            dummyObj.updateMatrix();
            instancedMesh.setMatrixAt(i, dummyObj.matrix);
        }

        globeGroup.add(instancedMesh);

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
                rotationVelY = 0.001;
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

            circleGeo.dispose();
            circleMat.dispose();
            instancedMesh.dispose();

            renderer.dispose();
        };
    }, []);

    return (
        <div
            ref={mountRef}
            style={{
                width: '1000px',
                height: '1000px',
                cursor: 'grab',
                margin: '0 auto',
            }}
        />
    );
}