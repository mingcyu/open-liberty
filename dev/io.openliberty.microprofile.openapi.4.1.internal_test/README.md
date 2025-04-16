This project just runs the same unit tests that we run for 4.0 using SmallRye OpenAPI 4.1.

We don't currently have any code difference between 4.0 and 4.1, all the changes are upstream in the API and SmallRye, but we need to test compatibility of our config serializer against the newer version of SmallRye.

If this test fails on a newer SmallRye OpenAPI 4.1.x version, we'll need a new Config Serializer implementation for 4.1.