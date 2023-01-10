#version 430 core

uniform vec3 eye;
uniform vec3 ray00;
uniform vec3 ray01;
uniform vec3 ray10;
uniform vec3 ray11;
uniform ivec2 iResolution;
uniform float iTime;

uniform ivec3 worldSize;

uniform bool doSpecular;
uniform bool doSkrunkle;
uniform bool doLightShadows;
uniform float ambiencePower;
uniform float lightingFalloff;
uniform float lightIntensity;

layout (std430, binding = 1) buffer Cubes {
    int cubes[];
};

layout (std430, binding = 3) buffer OutputImage {
    vec4 outputImage[];
};

vec4 intToColor(int b) {
    vec4 rgba = vec4(((b>>0)&0xFF), ((b>>8)&0xFF), ((b>>16)&0xFF), ((b>>24)&0xFF));
    rgba/=255;
    return rgba;
}

int index(ivec3 c) {
    return c.x + c.y * worldSize.x + c.z * worldSize.x * worldSize.y;
}

int getVoxel(ivec3 c) {
    return cubes[index(c)];
}

vec2 rotate2d(vec2 v, float a) {
    float sinA = sin(a);
    float cosA = cos(a);
    return vec2(v.x * cosA - v.y * sinA, v.y * cosA + v.x * sinA);
}

struct HitInfo {
    float distTravelled;
    vec3 normal;
    ivec3 block;
    int numSteps;
    vec3 pos;
    int colorInt;
    vec3 lightColor;
    vec3 blockColor;
    bool hitSomething;
};

const uint k = 1103515245U;
vec3 hash( uvec3 x ) {
    x = ((x>>8U)^x.yzx)*k;
    x = ((x>>8U)^x.yzx)*k;
    x = ((x>>8U)^x.yzx)*k;

    return vec3(x)*(1.0/float(0xffffffffU));
}

const int MATERIAL_AIR = 0;
const int MATERIAL_GLASS = 1;
const int MATERIAL_SOLID = 2;

HitInfo simpleRaycasting(vec3 rayPos, vec3 rayDir, int numSteps, int treatAsAir) {
    vec3 currBlock = floor(rayPos);
    vec3 deltaDist = abs(vec3(length(rayDir)) / rayDir);
    vec3 rayStep = sign(rayDir);
    vec3 sideDist = (sign(rayDir) * (vec3(currBlock) - rayPos) + (sign(rayDir) * 0.5) + 0.5) * deltaDist;
    vec3 mask;

    int i = 0;
    int colorInt;
    for (; i < numSteps; i++) {
        colorInt = getVoxel(ivec3(currBlock));
        if (colorInt != treatAsAir) {
            break;
        }
        mask = vec3(lessThanEqual(sideDist.xyz, min(sideDist.yzx, sideDist.zxy)));
        sideDist += mask * deltaDist;
        currBlock += mask * rayStep;
    }

    vec3 fixVal = (-rayPos + 0.5 - 0.5*vec3(rayStep));
    vec3 mini = (currBlock + fixVal)/rayDir;
    float t = max ( mini.x, max ( mini.y, mini.z ) );
    HitInfo hi;
    hi.distTravelled = t;
    hi.normal = -mask*rayStep;
    hi.block = ivec3(currBlock);
    hi.numSteps = i;
    hi.pos = rayDir*t;
    hi.colorInt = colorInt;

    return hi;
}

HitInfo raycast(vec3 originalRayPos, vec3 rayDirUnnormalized, vec3 lightColor, int numSteps, bool bendlight) {
    vec3 rayDir = normalize(rayDirUnnormalized);

    const float refractionIndex = 1.5;

    HitInfo hi;
    hi.lightColor = lightColor;
    hi.hitSomething = false;
    hi.distTravelled = 0.0;
    hi.numSteps = 0;
    hi.pos = originalRayPos;

    int currentlyInsideColor = getVoxel(ivec3(originalRayPos));
    vec3 currBlock = floor(hi.pos);
    const int maxQ = 10;
    int q = 0;
    for (; q < maxQ; q++) { // Number of material transitions the light can make
        HitInfo raycastResult = simpleRaycasting(hi.pos, rayDir, numSteps, currentlyInsideColor);

        hi.distTravelled += raycastResult.distTravelled;
        hi.normal = raycastResult.normal;
        hi.block = raycastResult.block;
        hi.numSteps += raycastResult.numSteps;
        hi.pos += rayDir*hi.distTravelled;
        hi.colorInt = raycastResult.colorInt;

        int previouslyInsideColor = currentlyInsideColor;
        currentlyInsideColor = hi.colorInt;

        if (previouslyInsideColor != 0) {
            vec4 localColor = intToColor(previouslyInsideColor);
            vec3 subtract = 1 - localColor.rgb;
            hi.lightColor *= 1 - (subtract * (hi.distTravelled * 0.1 + 0.5));
            //hi.lightColor *= localColor.rgb;
        }

        int material = MATERIAL_AIR;
        vec4 localColor;
        if (hi.colorInt != 0) {
            localColor = intToColor(hi.colorInt);
            hi.blockColor = localColor.rgb;
            if (localColor.a == 0) {
                material = MATERIAL_SOLID;
            } else {
                material = MATERIAL_GLASS;
            }
        }


        if (material == MATERIAL_SOLID) {
            hi.hitSomething = true;
            break;
        }

        if (!bendlight) {
            continue;
        }
        // We've not quit due to hitting something opaque; must have hit glass
        bool stayedInMaterial = false;

        ivec3 rando = ivec3(floor((hi.pos + hi.normal * 0.0001) * 4));
        vec3 h = hash(uvec3(rando));

        // We don't want to keep skrunkling if we're running out of iterations
        bool shouldSkrunkle = doSkrunkle && q < maxQ/2;
        vec3 warpedNormal = normalize(hi.normal + (1 - abs(hi.normal)) * h * (shouldSkrunkle ? 0.5 : 0));

        // We don't want to keep refracting if we're running out of iterations
        if (q < maxQ - 3) {
            if (material == MATERIAL_GLASS) {
                // entering glass
                rayDir = refract(rayDir, warpedNormal, 1/refractionIndex);
                stayedInMaterial = false;
            } else {
                // exiting glass
                vec3 refractDir = refract(rayDir, warpedNormal, refractionIndex);

                if (length(refractDir) > 0) {
                    rayDir = refractDir;
                    stayedInMaterial = false;
                } else {
                    rayDir = reflect(rayDir, warpedNormal);
                    stayedInMaterial = true;
                }
            }

            if (stayedInMaterial) {
                currentlyInsideColor = previouslyInsideColor;
                hi.pos += warpedNormal * 0.001;
                currBlock = floor(hi.pos);
            } else {
                hi.pos -= warpedNormal * 0.001;
            }
        }
    }

    if (q == maxQ) {
        hi.hitSomething = true;
    }

    hi.pos += hi.normal * 0.0001;

    return hi;
}

struct PointLight {
    vec3 pos;
    vec3 color;
    float power;
};

layout (local_size_x = 16, local_size_y = 16) in;
void main(void) {
    ivec2 fragCoord = ivec2(gl_GlobalInvocationID.xy);
    vec2 pos = (vec2(fragCoord) + vec2(0.5, 0.5)) / vec2(iResolution);
    vec3 rayDir = mix(mix(ray00, ray01, pos.y), mix(ray10, ray11, pos.y), pos.x);
    rayDir = normalize(rayDir);
    vec3 rayPos = eye;

    HitInfo primaryRay = raycast(rayPos, rayDir, vec3(1, 1, 1), 150, true);

    if (!primaryRay.hitSomething) {
        outputImage[iResolution.x * fragCoord.y + fragCoord.x] = vec4(primaryRay.lightColor, 1);
        return;
    }

    vec3 normal = primaryRay.normal;
    vec3 totalLight = vec3(0, 0, 0);

    vec3 ambientLight = vec3(1, 1, 1);
    if (abs(normal.x) != 0) {
        ambientLight *= 0.5;
    }
    if (abs(normal.y) != 0) {
        ambientLight *= 1.0;
    }
    if (abs(normal.z) == 0) {
        ambientLight *= 0.75;
    }

    vec3 sunlightDir = normalize(vec3(-0.5 - 0.5, 0.5, -1)); // normalize(vec3(sin(iTime) * 0.5 - 0.5, 0.5, -1));
    HitInfo windowLight = raycast(primaryRay.pos + sunlightDir * 55, -sunlightDir, vec3(1, 1, 1), 100, true);
    if (windowLight.distTravelled >= 55) {
        //totalLight += windowLight.lightColor * 2.5;
    }

    const int NUM_LIGHTS = 4;
    PointLight lights[NUM_LIGHTS] = {
    { vec3(5, 9, 32), vec3(1, 1, 1), 20 },
    { vec3(19.5, 20, 18), vec3(1, 1, 1), 10 },
    { vec3(2.5, 17.5, 24.5), vec3(1, 1, 1), 3 },
    { vec3(2.5, 17.5, 9.5), vec3(1, 1, 1), 3 },
    };

    vec3 h2 = hash(uvec3(ivec3(primaryRay.pos)));

    for (int lightId = 0; lightId < NUM_LIGHTS; lightId++) {
        PointLight light = lights[lightId];
        vec3 vec = primaryRay.pos - light.pos;
        float x = length(vec);
        vec = normalize(vec);

        HitInfo lightHi = raycast(light.pos, vec, light.color, 100, false);
        if (lightHi.distTravelled > x || !doLightShadows) {
            float distancePower = clamp(1/(pow(x, lightingFalloff) + 1), 0.0, 1.0); //  - sqrt(x) * 0.001
            float power = light.power * distancePower * lightIntensity;

            vec3 R = normalize(2 * dot(primaryRay.normal, vec) * primaryRay.normal - vec);
            float specular = pow(clamp(dot(R, rayDir), 0, 1), 8.0) * 0.5;

            totalLight += power * lightHi.lightColor;// * max(dot(primaryRay.normal, -vec), 0);

            if (doSpecular) {
                totalLight += power * specular * lightHi.lightColor;
            }
        }
    }

    totalLight += ambientLight * ambiencePower;

    vec3 color = primaryRay.blockColor * primaryRay.lightColor * totalLight * (1 + h2.r * 0.05);

    //color = pow( color, vec3(2.2) );

    outputImage[iResolution.x * fragCoord.y + fragCoord.x] = vec4(color, 1);
}