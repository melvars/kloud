export const cleanPath = (path: string) => {
    return path
        .replace("/files/", "")
        .replace("../", "") // TODO: Fix relative ../
        .replace("./", "")
        .replace(/([^:]\/)\/+/g, "$1");
};
