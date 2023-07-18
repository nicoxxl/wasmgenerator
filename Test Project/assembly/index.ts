function gen(x: i64, y: i64, z: i64, seed: i64): string {
  const CELL_SIZE: i64 = 20;
  let cellX = abs(x % CELL_SIZE);
  let cellZ = abs(z % CELL_SIZE);
  if (cellX === 0 && cellZ === 0) {
    return "minecraft:purpur_pillar";
  }
  if (
    (cellX == 0 || cellX === 1 || cellX === CELL_SIZE - 1) &&
    (cellZ == 0 || cellZ === 1 || cellZ === CELL_SIZE - 1)
  ) {
    return "minecraft:air";
  }
  const PYRAMID_SIZE = 300;
  if (abs(x) + abs(z) + y === PYRAMID_SIZE) {
    return "minecraft:grass_block";
  }
  if (abs(x) + abs(z) + y < PYRAMID_SIZE) {
    return "minecraft:dirt";
  }

  if (y < 64) {
    return "minecraft:dirt";
  } else if (y == 64) {
    return "minecraft:grass_block";
  }

  return "minecraft:air";
}

export function main(
  startx: i64,
  endx: i64,
  starty: i64,
  endy: i64,
  startz: i64,
  endz: i64,
  seed: i64
): string {
  let indices = new Map<string, i32>();
  let lastIdx: i32 = 0;
  let blockDef:string[] = [];
  let blocks = [];
  for (let x = startx; x < endx; x++) {
    for (let y = starty; y < endy; y++) {
      for (let z = startz; z < endz; z++) {
        let block:string = gen(x, y, z, seed);
        if (!indices.has(block)) {
          indices.set(block, lastIdx);
          blockDef.push(block);
          blocks.push(lastIdx);
          lastIdx += 1;
        } else {
          blocks.push(indices.get(block));
        }
      }
    }
  }
  return blockDef.join(';') + '|' + blocks.join(';');
}
